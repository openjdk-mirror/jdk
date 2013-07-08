/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include <X11/Xlib.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include "gtk2_interface.h"
#include "java_awt_Transparency.h"
#include "jvm_md.h"
#include "sizecalc.h"

#define MIN(a, b)  (((a) < (b)) ? (a) : (b))

#define CONV_BUFFER_SIZE 128

/* SynthConstants */
const gint ENABLED    = 1 << 0;
const gint MOUSE_OVER = 1 << 1;
const gint PRESSED    = 1 << 2;
const gint DISABLED   = 1 << 3;
const gint FOCUSED    = 1 << 8;
const gint SELECTED   = 1 << 9;
const gint DEFAULT    = 1 << 10;

static gboolean flag_g_thread_get_initialized = FALSE;

/* Widgets */
static GtkWidget *gtk2_widget = NULL;
static GtkWidget *gtk2_window = NULL;
static GtkFixed  *gtk2_fixed  = NULL;

/* Paint system */
static GdkPixmap *gtk2_white_pixmap = NULL;
static GdkPixmap *gtk2_black_pixmap = NULL;
static GdkPixbuf *gtk2_white_pixbuf = NULL;
static GdkPixbuf *gtk2_black_pixbuf = NULL;
static int gtk2_pixbuf_width = 0;
static int gtk2_pixbuf_height = 0;

/* Static buffer for conversion from java.lang.String to UTF-8 */
static char convertionBuffer[CONV_BUFFER_SIZE];

const char ENV_PREFIX[] = "GTK_MODULES=";
static gboolean initialised = FALSE;

/*******************/
enum GtkWidgetType
{
    _GTK_ARROW_TYPE,
    _GTK_BUTTON_TYPE,
    _GTK_CHECK_BUTTON_TYPE,
    _GTK_CHECK_MENU_ITEM_TYPE,
    _GTK_COLOR_SELECTION_DIALOG_TYPE,
    _GTK_COMBO_BOX_TYPE,
    _GTK_COMBO_BOX_ARROW_BUTTON_TYPE,
    _GTK_COMBO_BOX_TEXT_FIELD_TYPE,
    _GTK_CONTAINER_TYPE,
    _GTK_ENTRY_TYPE,
    _GTK_FRAME_TYPE,
    _GTK_HANDLE_BOX_TYPE,
    _GTK_HPANED_TYPE,
    _GTK_HPROGRESS_BAR_TYPE,
    _GTK_HSCALE_TYPE,
    _GTK_HSCROLLBAR_TYPE,
    _GTK_HSEPARATOR_TYPE,
    _GTK_IMAGE_TYPE,
    _GTK_MENU_TYPE,
    _GTK_MENU_BAR_TYPE,
    _GTK_MENU_ITEM_TYPE,
    _GTK_NOTEBOOK_TYPE,
    _GTK_LABEL_TYPE,
    _GTK_RADIO_BUTTON_TYPE,
    _GTK_RADIO_MENU_ITEM_TYPE,
    _GTK_SCROLLED_WINDOW_TYPE,
    _GTK_SEPARATOR_MENU_ITEM_TYPE,
    _GTK_SEPARATOR_TOOL_ITEM_TYPE,
    _GTK_SPIN_BUTTON_TYPE,
    _GTK_TEXT_VIEW_TYPE,
    _GTK_TOGGLE_BUTTON_TYPE,
    _GTK_TOOLBAR_TYPE,
    _GTK_TOOLTIP_TYPE,
    _GTK_TREE_VIEW_TYPE,
    _GTK_VIEWPORT_TYPE,
    _GTK_VPANED_TYPE,
    _GTK_VPROGRESS_BAR_TYPE,
    _GTK_VSCALE_TYPE,
    _GTK_VSCROLLBAR_TYPE,
    _GTK_VSEPARATOR_TYPE,
    _GTK_WINDOW_TYPE,
    _GTK_DIALOG_TYPE,
    _GTK_WIDGET_TYPE_SIZE
};

static GtkWidget *gtk2_widgets[_GTK_WIDGET_TYPE_SIZE];

/* Method bodies */
const char *getStrFor(JNIEnv *env, jstring val)
{
    int length = (*env)->GetStringLength(env, val);
    if (length > CONV_BUFFER_SIZE-1)
    {
        length = CONV_BUFFER_SIZE-1;
#ifdef INTERNAL_BUILD
        fprintf(stderr, "Note: Detail is too long: %d chars\n", length);
#endif /* INTERNAL_BUILD */
    }

    (*env)->GetStringUTFRegion(env, val, 0, length, convertionBuffer);
    return convertionBuffer;
}

static void throw_exception(JNIEnv *env, const char* name, const char* message)
{
    jclass class = (*env)->FindClass(env, name);

    if (class != NULL)
        (*env)->ThrowNew(env, class, message);

    (*env)->DeleteLocalRef(env, class);
}

gboolean gtk2_check_version()
{
#ifdef USE_SYSTEM_GTK
    return TRUE;
#else
    return gtk2_check_dlversion();
#endif
}

gboolean gtk2_load()
{
    gboolean result;
    int i;
    int (*handler)();
    int (*io_handler)();
    char *gtk_modules_env;

#ifndef USE_SYSTEM_GTK
    gtk2_dlload ();
#endif

    /*
     * Strip the AT-SPI GTK_MODULEs if present
     */
    gtk_modules_env = getenv ("GTK_MODULES");

    if (gtk_modules_env && strstr (gtk_modules_env, "atk-bridge") ||
        gtk_modules_env && strstr (gtk_modules_env, "gail"))
    {
        /* the new env will be smaller than the old one */
        gchar *s, *new_env = SAFE_SIZE_STRUCT_ALLOC(malloc,
                sizeof(ENV_PREFIX), 1, strlen (gtk_modules_env));

        if (new_env != NULL )
        {
            /* careful, strtok modifies its args */
            gchar *tmp_env = strdup (gtk_modules_env);
            strcpy(new_env, ENV_PREFIX);

            /* strip out 'atk-bridge' and 'gail' */
            size_t PREFIX_LENGTH = strlen(ENV_PREFIX);
            while (s = strtok(tmp_env, ":"))
            {
                if ((!strstr (s, "atk-bridge")) && (!strstr (s, "gail")))
                {
                    if (strlen (new_env) > PREFIX_LENGTH) {
                        new_env = strcat (new_env, ":");
                    }
                    new_env = strcat(new_env, s);
                }
                if (tmp_env)
                {
                    free (tmp_env);
                    tmp_env = NULL; /* next call to strtok arg1==NULL */
                }
            }
            putenv (new_env);
            free (new_env);
        }
    }

    /*
     * GTK should be initialized with gtk_init_check() before use.
     *
     * gtk_init_check installs its own error handlers. It is critical that
     * we preserve error handler set from AWT. Otherwise we'll crash on
     * BadMatch errors which we would normally ignore. The IO error handler
     * is preserved here, too, just for consistency.
    */
    handler = XSetErrorHandler(NULL);
    io_handler = XSetIOErrorHandler(NULL);

    if (gtk_check_version(2, 2, 0) == NULL) {
        // Init the thread system to use GLib in a thread-safe mode
        if (!flag_g_thread_get_initialized) {
            flag_g_thread_get_initialized = TRUE;

            g_thread_init(NULL);

            //According the GTK documentation, gdk_threads_init() should be
            //called before gtk_init() or gtk_init_check()
            gdk_threads_init();
        }
    }
    result = gtk_init_check (NULL, NULL);

    XSetErrorHandler(handler);
    XSetIOErrorHandler(io_handler);

    /* Initialize widget array. */
    for (i = 0; i < _GTK_WIDGET_TYPE_SIZE; i++)
    {
        gtk2_widgets[i] = NULL;
    }

    initialised = result;
    return result;
}

int gtk2_unload()
{
    int i;

    if (!initialised)
        return TRUE;

    /* Release painting objects */
    if (gtk2_white_pixmap != NULL) {
        g_object_unref (gtk2_white_pixmap);
        g_object_unref (gtk2_black_pixmap);
        g_object_unref (gtk2_white_pixbuf);
        g_object_unref (gtk2_black_pixbuf);
        gtk2_white_pixmap = gtk2_black_pixmap =
            gtk2_white_pixbuf = gtk2_black_pixbuf = NULL;
    }
    gtk2_pixbuf_width = 0;
    gtk2_pixbuf_height = 0;

    if (gtk2_window != NULL) {
        /* Destroying toplevel widget will destroy all contained widgets */
        gtk_widget_destroy (gtk2_window);

        /* Unset some static data so they get reinitialized on next load */
        gtk2_window = NULL;
    }

#ifdef USE_SYSTEM_GTK
    return TRUE;
#else
    return gtk2_dlunload ();
#endif
}

/* Dispatch all pending events from the GTK event loop.
 * This is needed to catch theme change and update widgets' style.
 */
void flush_gtk_event_loop()
{
    while( g_main_context_iteration (NULL, FALSE));
}

/*
 * Initialize components of containment hierarchy. This creates a GtkFixed
 * inside a GtkWindow. All widgets get realized.
 */
static void init_containers()
{
    if (gtk2_window == NULL)
    {
        gtk2_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
        gtk2_fixed = (GtkFixed *) gtk_fixed_new ();
        gtk_container_add ((GtkContainer*)gtk2_window,
			   (GtkWidget *)gtk2_fixed);
        gtk_widget_realize (gtk2_window);
        gtk_widget_realize ((GtkWidget *)gtk2_fixed);
    }
}

/*
 * Ensure everything is ready for drawing an element of the specified width
 * and height.
 *
 * We should somehow handle translucent images. GTK can draw to X Drawables
 * only, which don't support alpha. When we retrieve the image back from
 * the server, translucency information is lost. There're several ways to
 * work around this:
 * 1) Subclass GdkPixmap and cache translucent objects on client side. This
 * requires us to implement parts of X server drawing logic on client side.
 * Many X requests can potentially be "translucent"; e.g. XDrawLine with
 * fill=tile and a translucent tile is a "translucent" operation, whereas
 * XDrawLine with fill=solid is an "opaque" one. Moreover themes can (and some
 * do) intermix transparent and opaque operations which makes caching even
 * more problematic.
 * 2) Use Xorg 32bit ARGB visual when available. GDK has no native support
 * for it (as of version 2.6). Also even in JDS 3 Xorg does not support
 * these visuals by default, which makes optimizing for them pointless.
 * We can consider doing this at a later point when ARGB visuals become more
 * popular.
 * 3') GTK has plans to use Cairo as its graphical backend (presumably in
 * 2.8), and Cairo supports alpha. With it we could also get rid of the
 * unnecessary round trip to server and do all the drawing on client side.
 * 4) For now we draw to two different pixmaps and restore alpha channel by
 * comparing results. This can be optimized by using subclassed pixmap and
 * doing the second drawing only if necessary.
*/
void gtk2_init_painting(JNIEnv *env, gint width, gint height)
{
    GdkGC *gc;
    GdkPixbuf *white, *black;

    init_containers();

    if (gtk2_pixbuf_width < width || gtk2_pixbuf_height < height)
    {
        white = gdk_pixbuf_new (GDK_COLORSPACE_RGB, TRUE, 8, width, height);
        black = gdk_pixbuf_new (GDK_COLORSPACE_RGB, TRUE, 8, width, height);

        if (white == NULL || black == NULL)
        {
            snprintf(convertionBuffer, CONV_BUFFER_SIZE, "Couldn't create pixbuf of size %dx%d", width, height);
            throw_exception(env, "java/lang/RuntimeException", convertionBuffer);
            gdk_threads_leave();
            return;
        }

        if (gtk2_white_pixmap != NULL) {
            /* free old stuff */
            g_object_unref (gtk2_white_pixmap);
            g_object_unref (gtk2_black_pixmap);
            g_object_unref (gtk2_white_pixbuf);
            g_object_unref (gtk2_black_pixbuf);
        }

        gtk2_white_pixmap = gdk_pixmap_new (gtk2_window->window, width, height, -1);
        gtk2_black_pixmap = gdk_pixmap_new (gtk2_window->window, width, height, -1);

        gtk2_white_pixbuf = white;
        gtk2_black_pixbuf = black;

        gtk2_pixbuf_width = width;
        gtk2_pixbuf_height = height;
    }

    /* clear the pixmaps */
    gc = gdk_gc_new (gtk2_white_pixmap);
    gdk_rgb_gc_set_foreground (gc, 0xffffff);
    gdk_draw_rectangle (gtk2_white_pixmap, gc, TRUE, 0, 0, width, height);
    g_object_unref (gc);

    gc = gdk_gc_new (gtk2_black_pixmap);
    gdk_rgb_gc_set_foreground (gc, 0x000000);
    gdk_draw_rectangle (gtk2_black_pixmap, gc, TRUE, 0, 0, width, height);
    g_object_unref (gc);
}

/*
 * Restore image from white and black pixmaps and copy it into destination
 * buffer. This method compares two pixbufs taken from white and black
 * pixmaps and decodes color and alpha components. Pixbufs are RGB without
 * alpha, destination buffer is ABGR.
 *
 * The return value is the transparency type of the resulting image, either
 * one of java_awt_Transparency_OPAQUE, java_awt_Transparency_BITMASK, and
 * java_awt_Transparency_TRANSLUCENT.
 */
gint gtk2_copy_image(gint *dst, gint width, gint height)
{
    gint i, j, r, g, b;
    guchar *white, *black;
    gint stride, padding;
    gboolean is_opaque = TRUE;
    gboolean is_bitmask = TRUE;

    gdk_pixbuf_get_from_drawable (gtk2_white_pixbuf, gtk2_white_pixmap,
            NULL, 0, 0, 0, 0, width, height);
    gdk_pixbuf_get_from_drawable (gtk2_black_pixbuf, gtk2_black_pixmap,
            NULL, 0, 0, 0, 0, width, height);

    white = gdk_pixbuf_get_pixels (gtk2_white_pixbuf);
    black = gdk_pixbuf_get_pixels (gtk2_black_pixbuf);
    stride = gdk_pixbuf_get_rowstride (gtk2_black_pixbuf);
    padding = stride - width * 4;

    for (i = 0; i < height; i++) {
        for (j = 0; j < width; j++) {
            int r1 = *white++;
            int r2 = *black++;
            int alpha = 0xff + r2 - r1;

            switch (alpha) {
                case 0:       /* transparent pixel */
                    r = g = b = 0;
                    black += 3;
                    white += 3;
                    is_opaque = FALSE;
                    break;

                case 0xff:    /* opaque pixel */
                    r = r2;
                    g = *black++;
                    b = *black++;
                    black++;
                    white += 3;
                    break;

                default:      /* translucent pixel */
                    r = 0xff * r2 / alpha;
                    g = 0xff * *black++ / alpha;
                    b = 0xff * *black++ / alpha;
                    black++;
                    white += 3;
                    is_opaque = FALSE;
                    is_bitmask = FALSE;
                    break;
            }

            *dst++ = (alpha << 24 | r << 16 | g << 8 | b);
        }

        white += padding;
        black += padding;
    }
    return is_opaque ? java_awt_Transparency_OPAQUE :
                       (is_bitmask ? java_awt_Transparency_BITMASK :
                                     java_awt_Transparency_TRANSLUCENT);
}

static void
gtk2_set_direction(GtkWidget *widget, GtkTextDirection dir)
{
    /*
     * Some engines (inexplicably) look at the direction of the widget's
     * parent, so we need to set the direction of both the widget and its
     * parent.
     */
    gtk_widget_set_direction (widget, dir);
    if (widget->parent != NULL) {
        gtk_widget_set_direction (widget->parent, dir);
    }
}

/*
 * Initializes the widget to correct state for some engines.
 * This is a pure empirical method.
 */
static void init_toggle_widget(WidgetType widget_type, gint synth_state)
{
    gboolean is_active = ((synth_state & SELECTED) != 0);

    if (widget_type == RADIO_BUTTON ||
        widget_type == CHECK_BOX ||
        widget_type == TOGGLE_BUTTON) {
        ((GtkToggleButton*)gtk2_widget)->active = is_active;
    }

    if ((synth_state & FOCUSED) != 0) {
        ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_FOCUS;
    } else {
        ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_FOCUS;
    }

    if ((synth_state & MOUSE_OVER) != 0 && (synth_state & PRESSED) == 0 ||
           (synth_state & FOCUSED) != 0 && (synth_state & PRESSED) != 0) {
        gtk2_widget->state = GTK_STATE_PRELIGHT;
    } else if ((synth_state & DISABLED) != 0) {
        gtk2_widget->state = GTK_STATE_INSENSITIVE;
    } else {
        gtk2_widget->state = is_active ? GTK_STATE_ACTIVE : GTK_STATE_NORMAL;
    }
}

/* GTK state_type filter */
static GtkStateType get_gtk_state_type(WidgetType widget_type, gint synth_state)
{
    GtkStateType result = GTK_STATE_NORMAL;

    if ((synth_state & DISABLED) != 0) {
        result = GTK_STATE_INSENSITIVE;
    } else if ((synth_state & PRESSED) != 0) {
        result = GTK_STATE_ACTIVE;
    } else if ((synth_state & MOUSE_OVER) != 0) {
        result = GTK_STATE_PRELIGHT;
    }
    return result;
}

/* GTK shadow_type filter */
static GtkShadowType get_gtk_shadow_type(WidgetType widget_type, gint synth_state)
{
    GtkShadowType result = GTK_SHADOW_OUT;

    if ((synth_state & SELECTED) != 0) {
        result = GTK_SHADOW_IN;
    }
    return result;
}


static GtkWidget* gtk2_get_arrow(GtkArrowType arrow_type, GtkShadowType shadow_type)
{
    GtkWidget *arrow = NULL;
    if (NULL == gtk2_widgets[_GTK_ARROW_TYPE])
    {
        gtk2_widgets[_GTK_ARROW_TYPE] = gtk_arrow_new (arrow_type, shadow_type);
        gtk_container_add ((GtkContainer *)gtk2_fixed, gtk2_widgets[_GTK_ARROW_TYPE]);
        gtk_widget_realize (gtk2_widgets[_GTK_ARROW_TYPE]);
    }
    arrow = gtk2_widgets[_GTK_ARROW_TYPE];

    gtk_arrow_set (arrow, arrow_type, shadow_type);
    return arrow;
}

static GtkAdjustment* create_adjustment()
{
    return (GtkAdjustment *)
            gtk_adjustment_new (50.0, 0.0, 100.0, 10.0, 20.0, 20.0);
}

/**
 * Returns a pointer to the cached native widget for the specified widget
 * type.
 */
static GtkWidget *gtk2_get_widget(WidgetType widget_type)
{
    gboolean init_result = FALSE;
    GtkWidget *result = NULL;
    switch (widget_type)
    {
        case BUTTON:
        case TABLE_HEADER:
            if (init_result = (NULL == gtk2_widgets[_GTK_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_BUTTON_TYPE] = gtk_button_new ();
            }
            result = gtk2_widgets[_GTK_BUTTON_TYPE];
            break;
        case CHECK_BOX:
            if (init_result = (NULL == gtk2_widgets[_GTK_CHECK_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_CHECK_BUTTON_TYPE] =
                    gtk_check_button_new ();
            }
            result = gtk2_widgets[_GTK_CHECK_BUTTON_TYPE];
            break;
        case CHECK_BOX_MENU_ITEM:
            if (init_result = (NULL == gtk2_widgets[_GTK_CHECK_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_CHECK_MENU_ITEM_TYPE] =
                    gtk_check_menu_item_new ();
            }
            result = gtk2_widgets[_GTK_CHECK_MENU_ITEM_TYPE];
            break;
        /************************************************************
         *    Creation a dedicated color chooser is dangerous because
         * it deadlocks the EDT
         ************************************************************/
/*        case COLOR_CHOOSER:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE]))
            {
                gtk2_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE] =
                    gtk_color_selection_dialog_new (NULL);
            }
            result = gtk2_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE];
            break;*/
        case COMBO_BOX:
            if (init_result = (NULL == gtk2_widgets[_GTK_COMBO_BOX_TYPE]))
            {
                gtk2_widgets[_GTK_COMBO_BOX_TYPE] =
                    gtk_combo_box_new ();
            }
            result = gtk2_widgets[_GTK_COMBO_BOX_TYPE];
            break;
        case COMBO_BOX_ARROW_BUTTON:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE] =
                     gtk_toggle_button_new ();
            }
            result = gtk2_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE];
            break;
        case COMBO_BOX_TEXT_FIELD:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE]))
            {
                result = gtk2_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE] =
                     gtk_entry_new ();

                GtkSettings* settings = gtk_widget_get_settings(result);
                g_object_set(settings, "gtk-cursor-blink", FALSE, NULL);
            }
            result = gtk2_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE];
            break;
        case DESKTOP_ICON:
        case INTERNAL_FRAME_TITLE_PANE:
        case LABEL:
            if (init_result = (NULL == gtk2_widgets[_GTK_LABEL_TYPE]))
            {
                gtk2_widgets[_GTK_LABEL_TYPE] =
                    gtk_label_new (NULL);
            }
            result = gtk2_widgets[_GTK_LABEL_TYPE];
            break;
        case DESKTOP_PANE:
        case PANEL:
        case ROOT_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_CONTAINER_TYPE]))
            {
                /* There is no constructor for a container type.  I've
                 * choosen GtkFixed container since it has a default
                 * constructor.
                 */
                gtk2_widgets[_GTK_CONTAINER_TYPE] =
                    gtk_fixed_new();
            }
            result = gtk2_widgets[_GTK_CONTAINER_TYPE];
            break;
        case EDITOR_PANE:
        case TEXT_AREA:
        case TEXT_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_TEXT_VIEW_TYPE]))
            {
                gtk2_widgets[_GTK_TEXT_VIEW_TYPE] =
                    gtk_text_view_new();
            }
            result = gtk2_widgets[_GTK_TEXT_VIEW_TYPE];
            break;
        case FORMATTED_TEXT_FIELD:
        case PASSWORD_FIELD:
        case TEXT_FIELD:
            if (init_result = (NULL == gtk2_widgets[_GTK_ENTRY_TYPE]))
            {
                gtk2_widgets[_GTK_ENTRY_TYPE] =
                    gtk_entry_new ();

                GtkSettings* settings =
                    gtk_widget_get_settings(gtk2_widgets[_GTK_ENTRY_TYPE]);
                g_object_set(settings, "gtk-cursor-blink", FALSE, NULL);
            }
            result = gtk2_widgets[_GTK_ENTRY_TYPE];
            break;
        case HANDLE_BOX:
            if (init_result = (NULL == gtk2_widgets[_GTK_HANDLE_BOX_TYPE]))
            {
                gtk2_widgets[_GTK_HANDLE_BOX_TYPE] =
                    gtk_handle_box_new ();
            }
            result = gtk2_widgets[_GTK_HANDLE_BOX_TYPE];
            break;
        case HSCROLL_BAR:
        case HSCROLL_BAR_BUTTON_LEFT:
        case HSCROLL_BAR_BUTTON_RIGHT:
        case HSCROLL_BAR_TRACK:
        case HSCROLL_BAR_THUMB:
            if (init_result = (NULL == gtk2_widgets[_GTK_HSCROLLBAR_TYPE]))
            {
                gtk2_widgets[_GTK_HSCROLLBAR_TYPE] =
                    gtk_hscrollbar_new (create_adjustment());
            }
            result = gtk2_widgets[_GTK_HSCROLLBAR_TYPE];
            break;
        case HSEPARATOR:
            if (init_result = (NULL == gtk2_widgets[_GTK_HSEPARATOR_TYPE]))
            {
                gtk2_widgets[_GTK_HSEPARATOR_TYPE] =
                    gtk_hseparator_new ();
            }
            result = gtk2_widgets[_GTK_HSEPARATOR_TYPE];
            break;
        case HSLIDER:
        case HSLIDER_THUMB:
        case HSLIDER_TRACK:
            if (init_result = (NULL == gtk2_widgets[_GTK_HSCALE_TYPE]))
            {
                gtk2_widgets[_GTK_HSCALE_TYPE] =
		    gtk_hscale_new (NULL);
            }
            result = gtk2_widgets[_GTK_HSCALE_TYPE];
            break;
        case HSPLIT_PANE_DIVIDER:
        case SPLIT_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_HPANED_TYPE]))
            {
                gtk2_widgets[_GTK_HPANED_TYPE] = gtk_hpaned_new ();
            }
            result = gtk2_widgets[_GTK_HPANED_TYPE];
            break;
        case IMAGE:
            if (init_result = (NULL == gtk2_widgets[_GTK_IMAGE_TYPE]))
            {
                gtk2_widgets[_GTK_IMAGE_TYPE] = gtk_image_new ();
            }
            result = gtk2_widgets[_GTK_IMAGE_TYPE];
            break;
        case INTERNAL_FRAME:
            if (init_result = (NULL == gtk2_widgets[_GTK_WINDOW_TYPE]))
            {
                gtk2_widgets[_GTK_WINDOW_TYPE] =
                    gtk_window_new (GTK_WINDOW_TOPLEVEL);
            }
            result = gtk2_widgets[_GTK_WINDOW_TYPE];
            break;
        case TOOL_TIP:
            if (init_result = (NULL == gtk2_widgets[_GTK_TOOLTIP_TYPE]))
            {
                result = gtk_window_new (GTK_WINDOW_TOPLEVEL);
                gtk_widget_set_name (result, "gtk-tooltips");
                gtk2_widgets[_GTK_TOOLTIP_TYPE] = result;
            }
            result = gtk2_widgets[_GTK_TOOLTIP_TYPE];
            break;
        case LIST:
        case TABLE:
        case TREE:
        case TREE_CELL:
            if (init_result = (NULL == gtk2_widgets[_GTK_TREE_VIEW_TYPE]))
            {
                gtk2_widgets[_GTK_TREE_VIEW_TYPE] =
                    gtk_tree_view_new ();
            }
            result = gtk2_widgets[_GTK_TREE_VIEW_TYPE];
            break;
        case TITLED_BORDER:
            if (init_result = (NULL == gtk2_widgets[_GTK_FRAME_TYPE]))
            {
                gtk2_widgets[_GTK_FRAME_TYPE] = gtk_frame_new(NULL);
            }
            result = gtk2_widgets[_GTK_FRAME_TYPE];
            break;
        case POPUP_MENU:
            if (init_result = (NULL == gtk2_widgets[_GTK_MENU_TYPE]))
            {
                gtk2_widgets[_GTK_MENU_TYPE] =
                    gtk_menu_new ();
            }
            result = gtk2_widgets[_GTK_MENU_TYPE];
            break;
        case MENU:
        case MENU_ITEM:
        case MENU_ITEM_ACCELERATOR:
            if (init_result = (NULL == gtk2_widgets[_GTK_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_MENU_ITEM_TYPE] =
                    gtk_menu_item_new ();
            }
            result = gtk2_widgets[_GTK_MENU_ITEM_TYPE];
            break;
        case MENU_BAR:
            if (init_result = (NULL == gtk2_widgets[_GTK_MENU_BAR_TYPE]))
            {
                gtk2_widgets[_GTK_MENU_BAR_TYPE] =
                    gtk_menu_bar_new ();
            }
            result = gtk2_widgets[_GTK_MENU_BAR_TYPE];
            break;
        case COLOR_CHOOSER:
        case OPTION_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_DIALOG_TYPE]))
            {
                gtk2_widgets[_GTK_DIALOG_TYPE] =
                    gtk_dialog_new ();
            }
            result = gtk2_widgets[_GTK_DIALOG_TYPE];
            break;
        case POPUP_MENU_SEPARATOR:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE] =
                    gtk_separator_menu_item_new ();
            }
            result = gtk2_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE];
            break;
        case HPROGRESS_BAR:
            if (init_result = (NULL == gtk2_widgets[_GTK_HPROGRESS_BAR_TYPE]))
            {
                gtk2_widgets[_GTK_HPROGRESS_BAR_TYPE] =
                    gtk_progress_bar_new ();
            }
            result = gtk2_widgets[_GTK_HPROGRESS_BAR_TYPE];
            break;
        case VPROGRESS_BAR:
            if (init_result = (NULL == gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE]))
            {
                gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE] =
                    gtk_progress_bar_new ();
                /*
                 * Vertical JProgressBars always go bottom-to-top,
                 * regardless of the ComponentOrientation.
                 */
                gtk_progress_bar_set_orientation (
                    (GtkProgressBar *)gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE],
                    GTK_PROGRESS_BOTTOM_TO_TOP);
            }
            result = gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE];
            break;
        case RADIO_BUTTON:
            if (init_result = (NULL == gtk2_widgets[_GTK_RADIO_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_RADIO_BUTTON_TYPE] =
                    gtk_radio_button_new (NULL);
            }
            result = gtk2_widgets[_GTK_RADIO_BUTTON_TYPE];
            break;
        case RADIO_BUTTON_MENU_ITEM:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_RADIO_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_RADIO_MENU_ITEM_TYPE] =
                    gtk_radio_menu_item_new (NULL);
            }
            result = gtk2_widgets[_GTK_RADIO_MENU_ITEM_TYPE];
            break;
        case SCROLL_PANE:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_SCROLLED_WINDOW_TYPE]))
            {
                gtk2_widgets[_GTK_SCROLLED_WINDOW_TYPE] =
                    gtk_scrolled_window_new (NULL, NULL);
            }
            result = gtk2_widgets[_GTK_SCROLLED_WINDOW_TYPE];
            break;
        case SPINNER:
        case SPINNER_ARROW_BUTTON:
        case SPINNER_TEXT_FIELD:
            if (init_result = (NULL == gtk2_widgets[_GTK_SPIN_BUTTON_TYPE]))
            {
                result = gtk2_widgets[_GTK_SPIN_BUTTON_TYPE] =
                    gtk_spin_button_new (NULL, 0, 0);

                GtkSettings* settings = gtk_widget_get_settings(result);
                g_object_set(settings, "gtk-cursor-blink", FALSE, NULL);
            }
            result = gtk2_widgets[_GTK_SPIN_BUTTON_TYPE];
            break;
        case TABBED_PANE:
        case TABBED_PANE_TAB_AREA:
        case TABBED_PANE_CONTENT:
        case TABBED_PANE_TAB:
            if (init_result = (NULL == gtk2_widgets[_GTK_NOTEBOOK_TYPE]))
            {
                gtk2_widgets[_GTK_NOTEBOOK_TYPE] =
                    gtk_notebook_new ();
            }
            result = gtk2_widgets[_GTK_NOTEBOOK_TYPE];
            break;
        case TOGGLE_BUTTON:
            if (init_result = (NULL == gtk2_widgets[_GTK_TOGGLE_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_TOGGLE_BUTTON_TYPE] =
                    gtk_toggle_button_new ();
            }
            result = gtk2_widgets[_GTK_TOGGLE_BUTTON_TYPE];
            break;
        case TOOL_BAR:
        case TOOL_BAR_DRAG_WINDOW:
            if (init_result = (NULL == gtk2_widgets[_GTK_TOOLBAR_TYPE]))
            {
                gtk2_widgets[_GTK_TOOLBAR_TYPE] =
                    gtk_toolbar_new ();
            }
            result = gtk2_widgets[_GTK_TOOLBAR_TYPE];
            break;
        case TOOL_BAR_SEPARATOR:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE] =
                    gtk_separator_tool_item_new();
            }
            result = gtk2_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE];
            break;
        case VIEWPORT:
            if (init_result = (NULL == gtk2_widgets[_GTK_VIEWPORT_TYPE]))
            {
                GtkAdjustment *adjustment = create_adjustment();
                gtk2_widgets[_GTK_VIEWPORT_TYPE] =
                    gtk_viewport_new (adjustment, adjustment);
            }
            result = gtk2_widgets[_GTK_VIEWPORT_TYPE];
            break;
        case VSCROLL_BAR:
        case VSCROLL_BAR_BUTTON_UP:
        case VSCROLL_BAR_BUTTON_DOWN:
        case VSCROLL_BAR_TRACK:
        case VSCROLL_BAR_THUMB:
            if (init_result = (NULL == gtk2_widgets[_GTK_VSCROLLBAR_TYPE]))
            {
                gtk2_widgets[_GTK_VSCROLLBAR_TYPE] =
                    gtk_vscrollbar_new (create_adjustment());
            }
            result = gtk2_widgets[_GTK_VSCROLLBAR_TYPE];
            break;
        case VSEPARATOR:
            if (init_result = (NULL == gtk2_widgets[_GTK_VSEPARATOR_TYPE]))
            {
                gtk2_widgets[_GTK_VSEPARATOR_TYPE] =
                    gtk_vseparator_new ();
            }
            result = gtk2_widgets[_GTK_VSEPARATOR_TYPE];
            break;
        case VSLIDER:
        case VSLIDER_THUMB:
        case VSLIDER_TRACK:
            if (init_result = (NULL == gtk2_widgets[_GTK_VSCALE_TYPE]))
            {
                gtk2_widgets[_GTK_VSCALE_TYPE] =
                    gtk_vscale_new (NULL);
            }
            result = gtk2_widgets[_GTK_VSCALE_TYPE];
            /*
             * Vertical JSliders start at the bottom, while vertical
             * GtkVScale widgets start at the top (by default), so to fix
             * this we set the "inverted" flag to get the Swing behavior.
             */
            ((GtkRange*)result)->inverted = 1;
            break;
        case VSPLIT_PANE_DIVIDER:
            if (init_result = (NULL == gtk2_widgets[_GTK_VPANED_TYPE]))
            {
                gtk2_widgets[_GTK_VPANED_TYPE] = gtk_vpaned_new ();
            }
            result = gtk2_widgets[_GTK_VPANED_TYPE];
            break;
        default:
            result = NULL;
            break;
    }

    if (result != NULL && init_result)
    {
        if (widget_type == RADIO_BUTTON_MENU_ITEM ||
                widget_type == CHECK_BOX_MENU_ITEM ||
                widget_type == MENU_ITEM ||
                widget_type == MENU ||
                widget_type == POPUP_MENU_SEPARATOR)
        {
            GtkWidget *menu = gtk2_get_widget(POPUP_MENU);
            gtk_menu_shell_append ((GtkMenuShell *)menu, result);
        }
        else if (widget_type == POPUP_MENU)
        {
            GtkWidget *menu_bar = gtk2_get_widget(MENU_BAR);
            GtkWidget *root_menu = gtk_menu_item_new ();
            gtk_menu_item_set_submenu ((GtkMenuItem*)root_menu, result);
            gtk_menu_shell_append ((GtkMenuShell *)menu_bar, root_menu);
        }
        else if (widget_type == COMBO_BOX_ARROW_BUTTON ||
                 widget_type == COMBO_BOX_TEXT_FIELD)
        {
            /*
            * We add a regular GtkButton/GtkEntry to a GtkComboBoxEntry
            * in order to trick engines into thinking it's a real combobox
            * arrow button/text field.
            */
            GtkWidget *combo = gtk_combo_box_entry_new ();
	    gboolean use_new_combo;
	    
#ifdef USE_SYSTEM_GTK
	    use_new_combo = TRUE;
#else
	    use_new_combo = new_combo();
#endif

            if (use_new_combo && widget_type == COMBO_BOX_ARROW_BUTTON) {
                gtk_widget_set_parent (result, combo);
                ((GtkBin*)combo)->child = result;
            } else {
                gtk_container_add ((GtkContainer *)combo, result);
            }
            gtk_container_add ((GtkContainer *)gtk2_fixed, combo);
        }
        else if (widget_type != TOOL_TIP &&
                 widget_type != INTERNAL_FRAME &&
                 widget_type != OPTION_PANE)
        {
            gtk_container_add ((GtkContainer *)gtk2_fixed, result);
        }
        gtk_widget_realize (result);
    }
    return result;
}

void gtk2_paint_arrow(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkArrowType arrow_type, gboolean fill)
{
    static int w, h;
    static GtkRequisition size;

    if (widget_type == COMBO_BOX_ARROW_BUTTON || widget_type == TABLE)
        gtk2_widget = gtk2_get_arrow(arrow_type, shadow_type);
    else
        gtk2_widget = gtk2_get_widget(widget_type);

    switch (widget_type)
    {
        case SPINNER_ARROW_BUTTON:
            x = 1;
            y = ((arrow_type == GTK_ARROW_UP) ? 2 : 0);
            height -= 2;
            width -= 3;

            w = width / 2;
            w -= w % 2 - 1;
            h = (w + 1) / 2;
            break;

        case HSCROLL_BAR_BUTTON_LEFT:
        case HSCROLL_BAR_BUTTON_RIGHT:
        case VSCROLL_BAR_BUTTON_UP:
        case VSCROLL_BAR_BUTTON_DOWN:
            w = width / 2;
            h = height / 2;
            break;

        case COMBO_BOX_ARROW_BUTTON:
        case TABLE:
            x = 1;
            gtk_widget_size_request (gtk2_widget, &size);
            w = size.width - ((GtkMisc*)gtk2_widget)->xpad * 2;
            h = size.height - ((GtkMisc*)gtk2_widget)->ypad * 2;
            w = h = MIN(MIN(w, h), MIN(width,height)) * 0.7;
            break;

        default:
            w = width;
            h = height;
            break;
    }
    x += (width - w) / 2;
    y += (height - h) / 2;

    gtk_paint_arrow (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, arrow_type, fill,
            x, y, w, h);
    gtk_paint_arrow (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, arrow_type, fill,
            x, y, w, h);
}

void gtk2_paint_box(WidgetType widget_type, GtkStateType state_type,
                    GtkShadowType shadow_type, const gchar *detail,
                    gint x, gint y, gint width, gint height,
                    gint synth_state, GtkTextDirection dir)
{
    gtk2_widget = gtk2_get_widget(widget_type);

    /*
     * The clearlooks engine sometimes looks at the widget's state field
     * instead of just the state_type variable that we pass in, so to account
     * for those cases we set the widget's state field accordingly.  The
     * flags field is similarly important for things like focus/default state.
     */
    gtk2_widget->state = state_type;

    if (widget_type == HSLIDER_TRACK) {
        /*
         * For horizontal JSliders with right-to-left orientation, we need
         * to set the "inverted" flag to match the native GTK behavior where
         * the foreground highlight is on the right side of the slider thumb.
         * This is needed especially for the ubuntulooks engine, which looks
         * exclusively at the "inverted" flag to determine on which side of
         * the thumb to paint the highlight...
         */
        ((GtkRange*)gtk2_widget)->inverted = (dir == GTK_TEXT_DIR_RTL);

        /*
         * Note however that other engines like clearlooks will look at both
         * the "inverted" field and the text direction to determine how
         * the foreground highlight is painted:
         *     !inverted && ltr --> paint highlight on left side
         *     !inverted && rtl --> paint highlight on right side
         *      inverted && ltr --> paint highlight on right side
         *      inverted && rtl --> paint highlight on left side
         * So the only way to reliably get the desired results for horizontal
         * JSlider (i.e., highlight on left side for LTR ComponentOrientation
         * and highlight on right side for RTL ComponentOrientation) is to
         * always override text direction as LTR, and then set the "inverted"
         * flag accordingly (as we have done above).
         */
        dir = GTK_TEXT_DIR_LTR;
    }

    /*
     * Some engines (e.g. clearlooks) will paint the shadow of certain
     * widgets (e.g. COMBO_BOX_ARROW_BUTTON) differently depending on the
     * the text direction.
     */
    gtk2_set_direction(gtk2_widget, dir);

    switch (widget_type) {
    case BUTTON:
        if (synth_state & DEFAULT) {
            ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_DEFAULT;
        } else {
            ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_DEFAULT;
        }
        break;
    case TOGGLE_BUTTON:
        init_toggle_widget(widget_type, synth_state);
        break;
    case HSCROLL_BAR_BUTTON_LEFT:
        /*
         * The clearlooks engine will draw a "left" button when:
         *   x == w->allocation.x
         *
         * The ubuntulooks engine will draw a "left" button when:
         *   [x,y,width,height]
         *     intersects
         *   [w->alloc.x,w->alloc.y,width,height]
         *
         * The values that are set below should ensure that a "left"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x;
        gtk2_widget->allocation.y = y;
        gtk2_widget->allocation.width = width;
        gtk2_widget->allocation.height = height;
        break;
    case HSCROLL_BAR_BUTTON_RIGHT:
        /*
         * The clearlooks engine will draw a "right" button when:
         *   x + width == w->allocation.x + w->allocation.width
         *
         * The ubuntulooks engine will draw a "right" button when:
         *   [x,y,width,height]
         *     does not intersect
         *   [w->alloc.x,w->alloc.y,width,height]
         *     but does intersect
         *   [w->alloc.x+width,w->alloc.y,width,height]
         *
         * The values that are set below should ensure that a "right"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x+width;
        gtk2_widget->allocation.y = 0;
        gtk2_widget->allocation.width = 0;
        gtk2_widget->allocation.height = height;
        break;
    case VSCROLL_BAR_BUTTON_UP:
        /*
         * The clearlooks engine will draw an "up" button when:
         *   y == w->allocation.y
         *
         * The ubuntulooks engine will draw an "up" button when:
         *   [x,y,width,height]
         *     intersects
         *   [w->alloc.x,w->alloc.y,width,height]
         *
         * The values that are set below should ensure that an "up"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x;
        gtk2_widget->allocation.y = y;
        gtk2_widget->allocation.width = width;
        gtk2_widget->allocation.height = height;
        break;
    case VSCROLL_BAR_BUTTON_DOWN:
        /*
         * The clearlooks engine will draw a "down" button when:
         *   y + height == w->allocation.y + w->allocation.height
         *
         * The ubuntulooks engine will draw a "down" button when:
         *   [x,y,width,height]
         *     does not intersect
         *   [w->alloc.x,w->alloc.y,width,height]
         *     but does intersect
         *   [w->alloc.x,w->alloc.y+height,width,height]
         *
         * The values that are set below should ensure that a "down"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x;
        gtk2_widget->allocation.y = y+height;
        gtk2_widget->allocation.width = width;
        gtk2_widget->allocation.height = 0;
        break;
    default:
        break;
    }

    gtk_paint_box (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);
    gtk_paint_box (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);

    /*
     * Reset the text direction to the default value so that we don't
     * accidentally affect other operations and widgets.
     */
    gtk2_set_direction(gtk2_widget, GTK_TEXT_DIR_LTR);
}

void gtk2_paint_box_gap(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkPositionType gap_side, gint gap_x, gint gap_width)
{
    /* Clearlooks needs a real clip area to paint the gap properly */
    GdkRectangle area = { x, y, width, height };

    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_box_gap(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, &area, gtk2_widget, detail,
            x, y, width, height, gap_side, gap_x, gap_width);
    gtk_paint_box_gap(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, &area, gtk2_widget, detail,
            x, y, width, height, gap_side, gap_x, gap_width);
}

void gtk2_paint_check(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    GtkStateType state_type = get_gtk_state_type(widget_type, synth_state);
    GtkShadowType shadow_type = get_gtk_shadow_type(widget_type, synth_state);

    gtk2_widget = gtk2_get_widget(widget_type);
    init_toggle_widget(widget_type, synth_state);

    gtk_paint_check (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    gtk_paint_check (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

void gtk2_paint_diamond(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_diamond (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    gtk_paint_diamond (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

void gtk2_paint_expander(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height,
        GtkExpanderStyle expander_style)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_expander (gtk2_widget->style, gtk2_white_pixmap,
            state_type, NULL, gtk2_widget, detail,
            x + width / 2, y + height / 2, expander_style);
    gtk_paint_expander (gtk2_widget->style, gtk2_black_pixmap,
            state_type, NULL, gtk2_widget, detail,
            x + width / 2, y + height / 2, expander_style);
}

void gtk2_paint_extension(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkPositionType gap_side)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_extension (gtk2_widget->style, gtk2_white_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, gap_side);
    gtk_paint_extension (gtk2_widget->style, gtk2_black_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, gap_side);
}

void gtk2_paint_flat_box(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, gboolean has_focus)
{
    gtk2_widget = gtk2_get_widget(widget_type);

    if (has_focus)
        ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_FOCUS;
    else
        ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_FOCUS;

    gtk_paint_flat_box (gtk2_widget->style, gtk2_white_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    gtk_paint_flat_box (gtk2_widget->style, gtk2_black_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

void gtk2_paint_focus(WidgetType widget_type, GtkStateType state_type,
        const char *detail, gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_focus (gtk2_widget->style, gtk2_white_pixmap, state_type,
            NULL, gtk2_widget, detail, x, y, width, height);
    gtk_paint_focus (gtk2_widget->style, gtk2_black_pixmap, state_type,
            NULL, gtk2_widget, detail, x, y, width, height);
}

void gtk2_paint_handle(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_handle (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
    gtk_paint_handle (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
}

void gtk2_paint_hline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_hline (gtk2_widget->style, gtk2_white_pixmap, state_type,
            NULL, gtk2_widget, detail, x, x + width, y);
    gtk_paint_hline (gtk2_widget->style, gtk2_black_pixmap, state_type,
            NULL, gtk2_widget, detail, x, x + width, y);
}

void gtk2_paint_option(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    GtkStateType state_type = get_gtk_state_type(widget_type, synth_state);
    GtkShadowType shadow_type = get_gtk_shadow_type(widget_type, synth_state);

    gtk2_widget = gtk2_get_widget(widget_type);
    init_toggle_widget(widget_type, synth_state);

    gtk_paint_option (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    gtk_paint_option (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

void gtk2_paint_shadow(WidgetType widget_type, GtkStateType state_type,
                       GtkShadowType shadow_type, const gchar *detail,
                       gint x, gint y, gint width, gint height,
                       gint synth_state, GtkTextDirection dir)
{
    gtk2_widget = gtk2_get_widget(widget_type);

    /*
     * The clearlooks engine sometimes looks at the widget's state field
     * instead of just the state_type variable that we pass in, so to account
     * for those cases we set the widget's state field accordingly.  The
     * flags field is similarly important for things like focus state.
     */
    gtk2_widget->state = state_type;

    /*
     * Some engines (e.g. clearlooks) will paint the shadow of certain
     * widgets (e.g. COMBO_BOX_TEXT_FIELD) differently depending on the
     * the text direction.
     */
    gtk2_set_direction(gtk2_widget, dir);

    switch (widget_type) {
    case COMBO_BOX_TEXT_FIELD:
    case FORMATTED_TEXT_FIELD:
    case PASSWORD_FIELD:
    case SPINNER_TEXT_FIELD:
    case TEXT_FIELD:
        if (synth_state & FOCUSED) {
            ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_FOCUS;
        } else {
            ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_FOCUS;
        }
        break;
    default:
        break;
    }

    gtk_paint_shadow (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);
    gtk_paint_shadow (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);

    /*
     * Reset the text direction to the default value so that we don't
     * accidentally affect other operations and widgets.
     */
    gtk2_set_direction(gtk2_widget, GTK_TEXT_DIR_LTR);
}

void gtk2_paint_slider(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_slider (gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
    gtk_paint_slider (gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
}

void gtk2_paint_vline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_paint_vline (gtk2_widget->style, gtk2_white_pixmap, state_type,
            NULL, gtk2_widget, detail, y, y + height, x);
    gtk_paint_vline (gtk2_widget->style, gtk2_black_pixmap, state_type,
            NULL, gtk2_widget, detail, y, y + height, x);
}

void gtk_paint_background(WidgetType widget_type, GtkStateType state_type,
        gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    gtk_style_apply_default_background (gtk2_widget->style,
            gtk2_white_pixmap, TRUE, state_type, NULL, x, y, width, height);
    gtk_style_apply_default_background (gtk2_widget->style,
            gtk2_black_pixmap, TRUE, state_type, NULL, x, y, width, height);
}

GdkPixbuf *gtk2_get_stock_icon(gint widget_type, const gchar *stock_id,
        GtkIconSize size, GtkTextDirection direction, const char *detail)
{
    init_containers();
    gtk2_widget = gtk2_get_widget((widget_type < 0) ? IMAGE : widget_type);
    gtk2_widget->state = GTK_STATE_NORMAL;
    gtk_widget_set_direction (gtk2_widget, direction);
    return gtk_widget_render_icon (gtk2_widget, stock_id, size, detail);
}

/*************************************************/
gint gtk2_get_xthickness(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    GtkStyle* style = gtk2_widget->style;
    return style->xthickness;
}

gint gtk2_get_ythickness(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    GtkStyle* style = gtk2_widget->style;
    return style->ythickness;
}

/*************************************************/
guint8 recode_color(guint16 channel)
{
    return (guint8)(channel>>8);
}

gint gtk2_get_color_for_state(JNIEnv *env, WidgetType widget_type,
                              GtkStateType state_type, ColorType color_type)
{
    gint result = 0;
    GdkColor *color = NULL;

    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    GtkStyle* style = gtk2_widget->style;

    switch (color_type)
    {
        case FOREGROUND:
            color = &(style->fg[state_type]);
            break;
        case BACKGROUND:
            color = &(style->bg[state_type]);
            break;
        case TEXT_FOREGROUND:
            color = &(style->text[state_type]);
            break;
        case TEXT_BACKGROUND:
            color = &(style->base[state_type]);
            break;
        case LIGHT:
            color = &(style->light[state_type]);
            break;
        case DARK:
            color = &(style->dark[state_type]);
            break;
        case MID:
            color = &(style->mid[state_type]);
            break;
        case FOCUS:
        case BLACK:
            color = &(style->black);
            break;
        case WHITE:
            color = &(style->white);
            break;
    }

    if (color)
        result = recode_color(color->red)   << 16 |
                 recode_color(color->green) << 8  |
                 recode_color(color->blue);

    return result;
}

/*************************************************/
jobject create_Boolean(JNIEnv *env, jboolean boolean_value);
jobject create_Integer(JNIEnv *env, jint int_value);
jobject create_Long(JNIEnv *env, jlong long_value);
jobject create_Float(JNIEnv *env, jfloat float_value);
jobject create_Double(JNIEnv *env, jdouble double_value);
jobject create_Character(JNIEnv *env, jchar char_value);
jobject create_Insets(JNIEnv *env, GtkBorder *border);

jobject gtk2_get_class_value(JNIEnv *env, WidgetType widget_type, jstring jkey)
{
    init_containers();

    const char* key = getStrFor(env, jkey);
    gtk2_widget = gtk2_get_widget(widget_type);

    GValue value;
    value.g_type = 0;

    GParamSpec* param = gtk_widget_class_find_style_property (
                                    ((GTypeInstance*)gtk2_widget)->g_class, key);
    if( param )
    {
        g_value_init ( &value, param->value_type );
        gtk_widget_style_get_property (gtk2_widget, key, &value);

        if( g_type_is_a ( param->value_type, G_TYPE_BOOLEAN ))
        {
            gboolean val = g_value_get_boolean (&value);
            return create_Boolean(env, (jboolean)val);
        }
        else if( g_type_is_a ( param->value_type, G_TYPE_CHAR ))
        {
            gchar val = g_value_get_char (&value);
            return create_Character(env, (jchar)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_UCHAR ))
        {
            guchar val = g_value_get_uchar (&value);
            return create_Character(env, (jchar)val);
        }
        else if( g_type_is_a ( param->value_type, G_TYPE_INT ))
        {
            gint val = g_value_get_int (&value);
            return create_Integer(env, (jint)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_UINT ))
        {
            guint val = g_value_get_uint (&value);
            return create_Integer(env, (jint)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_LONG ))
        {
            glong val = g_value_get_long (&value);
            return create_Long(env, (jlong)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_ULONG ))
        {
            gulong val = g_value_get_ulong (&value);
            return create_Long(env, (jlong)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_INT64 ))
        {
            gint64 val = g_value_get_int64(&value);
            return create_Long(env, (jlong)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_UINT64 ))
        {
            guint64 val = g_value_get_uint64 (&value);
            return create_Long(env, (jlong)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_FLOAT ))
        {
            gfloat val = g_value_get_float (&value);
            return create_Float(env, (jfloat)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_DOUBLE ))
        {
            gdouble val = g_value_get_double (&value);
            return create_Double(env, (jdouble)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_ENUM ))
        {
            gint val = g_value_get_enum (&value);
            return create_Integer(env, (jint)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_FLAGS ))
        {
            guint val = g_value_get_flags (&value);
            return create_Integer(env, (jint)val);
        }
        else if( g_type_is_a( param->value_type, G_TYPE_STRING ))
        {
            const gchar* val = g_value_get_string (&value);

            /* We suppose that all values come in C locale and
             * utf-8 representation of a string is the same as
             * the string itself. If this isn't so we should
             * use g_convert.
             */
            return (*env)->NewStringUTF(env, val);
        }
        else if( g_type_is_a( param->value_type, GTK_TYPE_BORDER ))
        {
            GtkBorder *border = (GtkBorder*)g_value_get_boxed(&value);
            return border ? create_Insets(env, border) : NULL;
        }

        /*      TODO: Other types are not supported yet.*/
/*        else if( g_type_is_a( param->value_type, G_TYPE_PARAM ))
        {
            GParamSpec* val = g_value_get_param (&value);
            printf( "Param: %p\n", val );
        }
        else if( g_type_is_a( param->value_type, G_TYPE_BOXED ))
        {
            gpointer* val = g_value_get_boxed (&value);
            printf( "Boxed: %p\n", val );
        }
        else if( g_type_is_a( param->value_type, G_TYPE_POINTER ))
        {
            gpointer* val = g_value_get_pointer (&value);
            printf( "Pointer: %p\n", val );
        }
        else if( g_type_is_a( param->value_type, G_TYPE_OBJECT ))
        {
            GObject* val = (GObject*)g_value_get_object (&value);
            printf( "Object: %p\n", val );
        }*/
    }

    return NULL;
}

void gtk2_set_range_value(WidgetType widget_type, jdouble value,
                          jdouble min, jdouble max, jdouble visible)
{
    GtkAdjustment *adj;

    gtk2_widget = gtk2_get_widget(widget_type);

    adj = gtk_range_get_adjustment ((GtkRange *)gtk2_widget);
    adj->value = (gdouble)value;
    adj->lower = (gdouble)min;
    adj->upper = (gdouble)max;
    adj->page_size = (gdouble)visible;
}

/*************************************************/
jobject create_Object(JNIEnv *env, jmethodID *cid,
                             const char* class_name,
                             const char* signature,
                             jvalue* value)
{
    jclass  class;
    jobject result;

    class = (*env)->FindClass(env, class_name);
    if( class == NULL )
        return NULL; /* can't find/load the class, exception thrown */

    if( *cid == NULL)
    {
        *cid = (*env)->GetMethodID(env, class, "<init>", signature);
        if( *cid == NULL )
        {
            (*env)->DeleteLocalRef(env, class);
            return NULL; /* can't find/get the method, exception thrown */
        }
    }

    result = (*env)->NewObjectA(env, class, *cid, value);

    (*env)->DeleteLocalRef(env, class);
    return result;
}

jobject create_Boolean(JNIEnv *env, jboolean boolean_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.z = boolean_value;

    return create_Object(env, &cid, "java/lang/Boolean", "(Z)V", &value);
}

jobject create_Integer(JNIEnv *env, jint int_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.i = int_value;

    return create_Object(env, &cid, "java/lang/Integer", "(I)V", &value);
}

jobject create_Long(JNIEnv *env, jlong long_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.j = long_value;

    return create_Object(env, &cid, "java/lang/Long", "(J)V", &value);
}

jobject create_Float(JNIEnv *env, jfloat float_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.f = float_value;

    return create_Object(env, &cid, "java/lang/Float", "(F)V", &value);
}

jobject create_Double(JNIEnv *env, jdouble double_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.d = double_value;

    return create_Object(env, &cid, "java/lang/Double", "(D)V", &value);
}

jobject create_Character(JNIEnv *env, jchar char_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.c = char_value;

    return create_Object(env, &cid, "java/lang/Character", "(C)V", &value);
}


jobject create_Insets(JNIEnv *env, GtkBorder *border)
{
    static jmethodID cid = NULL;
    jvalue values[4];

    values[0].i = border->top;
    values[1].i = border->left;
    values[2].i = border->bottom;
    values[3].i = border->right;

    return create_Object(env, &cid, "java/awt/Insets", "(IIII)V", values);
}

/*********************************************/
jstring gtk2_get_pango_font_name(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    jstring  result = NULL;
    GtkStyle* style = gtk2_widget->style;

    if (style && style->font_desc)
    {
        gchar* val = pango_font_description_to_string (style->font_desc);
        result = (*env)->NewStringUTF(env, val);
        g_free( val );
    }

    return result;
}

/***********************************************/
jobject get_string_property(JNIEnv *env, GtkSettings* settings, const gchar* key)
{
    jobject result = NULL;
    gchar*  strval = NULL;

    g_object_get (settings, key, &strval, NULL);
    result = (*env)->NewStringUTF(env, strval);
    g_free (strval);

    return result;
}
/*
jobject get_integer_property(JNIEnv *env, GtkSettings* settings, const gchar* key)
{
    gint    intval = NULL;

    g_object_get (settings, key, &intval, NULL);
    return create_Integer(env, intval);
}*/

jobject get_boolean_property(JNIEnv* env, GtkSettings* settings, const gchar *key)
{
    gboolean boolval = NULL;

    g_object_get (settings, key, &boolval, NULL);
    return create_Boolean(env, (jboolean) boolval);
}

jobject gtk2_get_setting(JNIEnv *env, Setting property)
{
    GtkSettings* settings = gtk_settings_get_default ();

    switch (property)
    {
        case GTK_FONT_NAME:
            return get_string_property(env, settings, "gtk-font-name");
        case GTK_ICON_SIZES:
            return get_string_property(env, settings, "gtk-icon-sizes");
        case GTK_BUTTON_ORDER:
	    return get_boolean_property(env, settings, "gtk-alternative-button-order");
    }

    return NULL;
}
