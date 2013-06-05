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
#ifndef _GTK2_INTERFACE_H
#define _GTK2_INTERFACE_H

#include <stdlib.h>
#include <jni.h>

#include <gtk/gtk.h>

#ifndef USE_SYSTEM_GTK
#include <gtk_fp.h>
#endif

typedef enum _WidgetType
{
    BUTTON,                     /* GtkButton */
    CHECK_BOX,                  /* GtkCheckButton */
    CHECK_BOX_MENU_ITEM,        /* GtkCheckMenuItem */
    COLOR_CHOOSER,              /* GtkColorSelectionDialog */
    COMBO_BOX,                  /* GtkComboBox */
    COMBO_BOX_ARROW_BUTTON,     /* GtkComboBoxEntry */
    COMBO_BOX_TEXT_FIELD,       /* GtkComboBoxEntry */
    DESKTOP_ICON,               /* GtkLabel */
    DESKTOP_PANE,               /* GtkContainer */
    EDITOR_PANE,                /* GtkTextView */
    FORMATTED_TEXT_FIELD,       /* GtkEntry */
    HANDLE_BOX,                 /* GtkHandleBox */
    HPROGRESS_BAR,              /* GtkProgressBar */
    HSCROLL_BAR,                /* GtkHScrollbar */
    HSCROLL_BAR_BUTTON_LEFT,    /* GtkHScrollbar */
    HSCROLL_BAR_BUTTON_RIGHT,   /* GtkHScrollbar */
    HSCROLL_BAR_TRACK,          /* GtkHScrollbar */
    HSCROLL_BAR_THUMB,          /* GtkHScrollbar */
    HSEPARATOR,                 /* GtkHSeparator */
    HSLIDER,                    /* GtkHScale */
    HSLIDER_TRACK,              /* GtkHScale */
    HSLIDER_THUMB,              /* GtkHScale */
    HSPLIT_PANE_DIVIDER,        /* GtkHPaned */
    INTERNAL_FRAME,             /* GtkWindow */
    INTERNAL_FRAME_TITLE_PANE,  /* GtkLabel */
    IMAGE,                      /* GtkImage */
    LABEL,                      /* GtkLabel */
    LIST,                       /* GtkTreeView */
    MENU,                       /* GtkMenu */
    MENU_BAR,                   /* GtkMenuBar */
    MENU_ITEM,                  /* GtkMenuItem */
    MENU_ITEM_ACCELERATOR,      /* GtkLabel */
    OPTION_PANE,                /* GtkMessageDialog */
    PANEL,                      /* GtkContainer */
    PASSWORD_FIELD,             /* GtkEntry */
    POPUP_MENU,                 /* GtkMenu */
    POPUP_MENU_SEPARATOR,       /* GtkSeparatorMenuItem */
    RADIO_BUTTON,               /* GtkRadioButton */
    RADIO_BUTTON_MENU_ITEM,     /* GtkRadioMenuItem */
    ROOT_PANE,                  /* GtkContainer */
    SCROLL_PANE,                /* GtkScrolledWindow */
    SPINNER,                    /* GtkSpinButton */
    SPINNER_ARROW_BUTTON,       /* GtkSpinButton */
    SPINNER_TEXT_FIELD,         /* GtkSpinButton */
    SPLIT_PANE,                 /* GtkPaned */
    TABBED_PANE,                /* GtkNotebook */
    TABBED_PANE_TAB_AREA,       /* GtkNotebook */
    TABBED_PANE_CONTENT,        /* GtkNotebook */
    TABBED_PANE_TAB,            /* GtkNotebook */
    TABLE,                      /* GtkTreeView */
    TABLE_HEADER,               /* GtkButton */
    TEXT_AREA,                  /* GtkTextView */
    TEXT_FIELD,                 /* GtkEntry */
    TEXT_PANE,                  /* GtkTextView */
    TITLED_BORDER,              /* GtkFrame */
    TOGGLE_BUTTON,              /* GtkToggleButton */
    TOOL_BAR,                   /* GtkToolbar */
    TOOL_BAR_DRAG_WINDOW,       /* GtkToolbar */
    TOOL_BAR_SEPARATOR,         /* GtkSeparatorToolItem */
    TOOL_TIP,                   /* GtkWindow */
    TREE,                       /* GtkTreeView */
    TREE_CELL,                  /* GtkTreeView */
    VIEWPORT,                   /* GtkViewport */
    VPROGRESS_BAR,              /* GtkProgressBar */
    VSCROLL_BAR,                /* GtkVScrollbar */
    VSCROLL_BAR_BUTTON_UP,      /* GtkVScrollbar */
    VSCROLL_BAR_BUTTON_DOWN,    /* GtkVScrollbar */
    VSCROLL_BAR_TRACK,          /* GtkVScrollbar */
    VSCROLL_BAR_THUMB,          /* GtkVScrollbar */
    VSEPARATOR,                 /* GtkVSeparator */
    VSLIDER,                    /* GtkVScale */
    VSLIDER_TRACK,              /* GtkVScale */
    VSLIDER_THUMB,              /* GtkVScale */
    VSPLIT_PANE_DIVIDER,        /* GtkVPaned */
    WIDGET_TYPE_SIZE
} WidgetType;

typedef enum _ColorType
{
    FOREGROUND,
    BACKGROUND,
    TEXT_FOREGROUND,
    TEXT_BACKGROUND,
    FOCUS,
    LIGHT,
    DARK,
    MID,
    BLACK,
    WHITE
} ColorType;

typedef enum _Setting
{
    GTK_FONT_NAME,
    GTK_ICON_SIZES,
    GTK_BUTTON_ORDER
} Setting;

/*
 * Converts java.lang.String object to UTF-8 character string.
 */
const char *getStrFor(JNIEnv *env, jstring value);

/*
 * Check whether the gtk2 library is available and meets the minimum
 * version requirement.  If the library is already loaded this method has no
 * effect and returns success.
 * Returns FALSE on failure and TRUE on success.
 */
gboolean gtk2_check_version();

/*
 * Load the gtk2 library.  If the library is already loaded this method has no
 * effect and returns success.
 * Returns FALSE on failure and TRUE on success.
 */
gboolean gtk2_load();

/*
 * Unload the gtk2 library.  If the library is already unloaded this method has
 * no effect and returns success.
 * Returns FALSE on failure and TRUE on success.
 */
gboolean gtk2_unload();

void gtk2_paint_arrow(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkArrowType arrow_type, gboolean fill);
void gtk2_paint_box(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        gint synth_state, GtkTextDirection dir);
void gtk2_paint_box_gap(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkPositionType gap_side, gint gap_x, gint gap_width);
void gtk2_paint_check(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height);
void gtk2_paint_diamond(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height);
void gtk2_paint_expander(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height,
        GtkExpanderStyle expander_style);
void gtk2_paint_extension(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkPositionType gap_side);
void gtk2_paint_flat_box(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, gboolean has_focus);
void gtk2_paint_focus(WidgetType widget_type, GtkStateType state_type,
        const char *detail, gint x, gint y, gint width, gint height);
void gtk2_paint_handle(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation);
void gtk2_paint_hline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height);
void gtk2_paint_option(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height);
void gtk2_paint_shadow(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        gint synth_state, GtkTextDirection dir);
void gtk2_paint_slider(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation);
void gtk2_paint_vline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height);
void gtk_paint_background(WidgetType widget_type, GtkStateType state_type,
        gint x, gint y, gint width, gint height);

void gtk2_init_painting(JNIEnv *env, gint w, gint h);
gint gtk2_copy_image(gint *dest, gint width, gint height);

gint gtk2_get_xthickness(JNIEnv *env, WidgetType widget_type);
gint gtk2_get_ythickness(JNIEnv *env, WidgetType widget_type);
gint gtk2_get_color_for_state(JNIEnv *env, WidgetType widget_type,
                              GtkStateType state_type, ColorType color_type);
jobject gtk2_get_class_value(JNIEnv *env, WidgetType widget_type, jstring key);

GdkPixbuf *gtk2_get_stock_icon(gint widget_type, const gchar *stock_id,
        GtkIconSize size, GtkTextDirection direction, const char *detail);
GdkPixbuf *gtk2_get_icon(const gchar *filename, gint size);
jstring gtk2_get_pango_font_name(JNIEnv *env, WidgetType widget_type);

void flush_gtk_event_loop();

jobject gtk2_get_setting(JNIEnv *env, Setting property);

void gtk2_set_range_value(WidgetType widget_type, jdouble value,
                          jdouble min, jdouble max, jdouble visible);

#endif /* !_GTK2_INTERFACE_H */
