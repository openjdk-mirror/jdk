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
#ifndef __GTK_FP_H__
#define __GTK_FP_H__

#include <gtk/gtk.h>

gboolean gtk2_check_dlversion();
gboolean gtk2_dlload();
int gtk2_dlunload();
gboolean new_combo();

/*************************
 * Glib function pointers
 *************************/

gboolean (*fp_g_main_context_iteration)(GMainContext *context,
                                             gboolean may_block);

GValue*      (*fp_g_value_init)(GValue *value, GType g_type);
gboolean     (*fp_g_type_is_a)(GType type, GType is_a_type);
gboolean     (*fp_g_value_get_boolean)(const GValue *value);
gchar        (*fp_g_value_get_char)(const GValue *value);
guchar       (*fp_g_value_get_uchar)(const GValue *value);
gint         (*fp_g_value_get_int)(const GValue *value);
guint        (*fp_g_value_get_uint)(const GValue *value);
glong        (*fp_g_value_get_long)(const GValue *value);
gulong       (*fp_g_value_get_ulong)(const GValue *value);
gint64       (*fp_g_value_get_int64)(const GValue *value);
guint64      (*fp_g_value_get_uint64)(const GValue *value);
gfloat       (*fp_g_value_get_float)(const GValue *value);
gdouble      (*fp_g_value_get_double)(const GValue *value);
const gchar* (*fp_g_value_get_string)(const GValue *value);
gint         (*fp_g_value_get_enum)(const GValue *value);
guint        (*fp_g_value_get_flags)(const GValue *value);
GParamSpec*  (*fp_g_value_get_param)(const GValue *value);
gpointer*    (*fp_g_value_get_boxed)(const GValue *value);
gpointer*    (*fp_g_value_get_pointer)(const GValue *value);
GObject*     (*fp_g_value_get_object)(const GValue *value);
GParamSpec*  (*fp_g_param_spec_int)(const gchar *name,
        const gchar *nick, const gchar *blurb,
        gint minimum, gint maximum, gint default_value,
        GParamFlags flags);
void         (*fp_g_object_get)(gpointer object,
                                       const gchar* fpn, ...);
void         (*fp_g_object_set)(gpointer object,
                                       const gchar *first_property_name,
                                       ...);
/************************
 * GDK function pointers
 ************************/
GdkPixmap *(*fp_gdk_pixmap_new)(GdkDrawable *drawable,
        gint width, gint height, gint depth);
GdkGC *(*fp_gdk_gc_new)(GdkDrawable*);
void (*fp_gdk_rgb_gc_set_foreground)(GdkGC*, guint32);
void (*fp_gdk_draw_rectangle)(GdkDrawable*, GdkGC*, gboolean,
        gint, gint, gint, gint);
GdkPixbuf *(*fp_gdk_pixbuf_new)(GdkColorspace colorspace,
        gboolean has_alpha, int bits_per_sample, int width, int height);
GdkPixbuf *(*fp_gdk_pixbuf_get_from_drawable)(GdkPixbuf *dest,
        GdkDrawable *src, GdkColormap *cmap, int src_x, int src_y,
        int dest_x, int dest_y, int width, int height);
void (*fp_gdk_drawable_get_size)(GdkDrawable *drawable,
        gint* width, gint* height);

/************************
 * Gtk function pointers
 ************************/
gboolean (*fp_gtk_init_check)(int* argc, char** argv);

/* Painting */
void (*fp_gtk_paint_hline)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint x1, gint x2, gint y);
void (*fp_gtk_paint_vline)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint y1, gint y2, gint x);
void (*fp_gtk_paint_shadow)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_arrow)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        GtkArrowType arrow_type, gboolean fill, gint x, gint y,
        gint width, gint height);
void (*fp_gtk_paint_diamond)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_box)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_flat_box)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_check)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_option)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_box_gap)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height,
        GtkPositionType gap_side, gint gap_x, gint gap_width);
void (*fp_gtk_paint_extension)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height, GtkPositionType gap_side);
void (*fp_gtk_paint_focus)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint x, gint y, gint width, gint height);
void (*fp_gtk_paint_slider)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation);
void (*fp_gtk_paint_handle)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation);
void (*fp_gtk_paint_expander)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint x, gint y, GtkExpanderStyle expander_style);
void (*fp_gtk_style_apply_default_background)(GtkStyle* style,
        GdkWindow* window, gboolean set_bg, GtkStateType state_type,
        GdkRectangle* area, gint x, gint y, gint width, gint height);

/* Widget creation */
GtkWidget* (*fp_gtk_arrow_new)(GtkArrowType arrow_type,
                                      GtkShadowType shadow_type);
GtkWidget* (*fp_gtk_button_new)();
GtkWidget* (*fp_gtk_check_button_new)();
GtkWidget* (*fp_gtk_check_menu_item_new)();
GtkWidget* (*fp_gtk_color_selection_dialog_new)(const gchar* title);
GtkWidget* (*fp_gtk_combo_box_new)();
GtkWidget* (*fp_gtk_combo_box_entry_new)();
GtkWidget* (*fp_gtk_entry_new)();
GtkWidget* (*fp_gtk_fixed_new)();
GtkWidget* (*fp_gtk_handle_box_new)();
GtkWidget* (*fp_gtk_hpaned_new)();
GtkWidget* (*fp_gtk_vpaned_new)();
GtkWidget* (*fp_gtk_hscale_new)(GtkAdjustment* adjustment);
GtkWidget* (*fp_gtk_vscale_new)(GtkAdjustment* adjustment);
GtkWidget* (*fp_gtk_hscrollbar_new)(GtkAdjustment* adjustment);
GtkWidget* (*fp_gtk_vscrollbar_new)(GtkAdjustment* adjustment);
GtkWidget* (*fp_gtk_hseparator_new)();
GtkWidget* (*fp_gtk_vseparator_new)();
GtkWidget* (*fp_gtk_image_new)();
GtkWidget* (*fp_gtk_label_new)(const gchar* str);
GtkWidget* (*fp_gtk_menu_new)();
GtkWidget* (*fp_gtk_menu_bar_new)();
GtkWidget* (*fp_gtk_menu_item_new)();
GtkWidget* (*fp_gtk_notebook_new)();
GtkWidget* (*fp_gtk_progress_bar_new)();
GtkWidget* (*fp_gtk_progress_bar_set_orientation)(
        GtkProgressBar *pbar,
        GtkProgressBarOrientation orientation);
GtkWidget* (*fp_gtk_radio_button_new)(GSList *group);
GtkWidget* (*fp_gtk_radio_menu_item_new)(GSList *group);
GtkWidget* (*fp_gtk_scrolled_window_new)(GtkAdjustment *hadjustment,
        GtkAdjustment *vadjustment);
GtkWidget* (*fp_gtk_separator_menu_item_new)();
GtkWidget* (*fp_gtk_separator_tool_item_new)();
GtkWidget* (*fp_gtk_text_view_new)();
GtkWidget* (*fp_gtk_toggle_button_new)();
GtkWidget* (*fp_gtk_toolbar_new)();
GtkWidget* (*fp_gtk_tree_view_new)();
GtkWidget* (*fp_gtk_viewport_new)(GtkAdjustment *hadjustment,
        GtkAdjustment *vadjustment);
GtkWidget* (*fp_gtk_window_new)(GtkWindowType type);
GtkWidget* (*fp_gtk_dialog_new)();
GtkWidget* (*fp_gtk_spin_button_new)(GtkAdjustment *adjustment,
        gdouble climb_rate, guint digits);
GtkWidget* (*fp_gtk_frame_new)(const gchar *label);

/* Other widget operations */
GtkObject* (*fp_gtk_adjustment_new)(gdouble value,
        gdouble lower, gdouble upper, gdouble step_increment,
        gdouble page_increment, gdouble page_size);
void (*fp_gtk_container_add)(GtkContainer *window, GtkWidget *widget);
void (*fp_gtk_menu_shell_append)(GtkMenuShell *menu_shell,
        GtkWidget *child);
void (*fp_gtk_menu_item_set_submenu)(GtkMenuItem *menu_item,
        GtkWidget *submenu);
void (*fp_gtk_widget_realize)(GtkWidget *widget);
GdkPixbuf* (*fp_gtk_widget_render_icon)(GtkWidget *widget,
        const gchar *stock_id, GtkIconSize size, const gchar *detail);
void (*fp_gtk_widget_set_name)(GtkWidget *widget, const gchar *name);
void (*fp_gtk_widget_set_parent)(GtkWidget *widget, GtkWidget *parent);
void (*fp_gtk_widget_set_direction)(GtkWidget *widget,
        GtkTextDirection direction);
void (*fp_gtk_widget_style_get)(GtkWidget *widget,
        const gchar *first_property_name, ...);
void (*fp_gtk_widget_class_install_style_property)(
        GtkWidgetClass* class, GParamSpec *pspec);
GParamSpec* (*fp_gtk_widget_class_find_style_property)(
        GtkWidgetClass* class, const gchar* property_name);
void (*fp_gtk_widget_style_get_property)(GtkWidget* widget,
        const gchar* property_name, GValue* value);
char* (*fp_pango_font_description_to_string)(
        const PangoFontDescription* fd);
GtkSettings* (*fp_gtk_settings_get_default)();
GtkSettings* (*fp_gtk_widget_get_settings)(GtkWidget *widget);
GType        (*fp_gtk_border_get_type)();
void (*fp_gtk_arrow_set)(GtkWidget* arrow,
                                GtkArrowType arrow_type,
                                GtkShadowType shadow_type);
void (*fp_gtk_widget_size_request)(GtkWidget *widget,
                                          GtkRequisition *requisition);
GtkAdjustment* (*fp_gtk_range_get_adjustment)(GtkRange* range);

void (*fp_g_free)(gpointer mem);
void (*fp_g_object_unref)(gpointer object);
int (*fp_gdk_pixbuf_get_bits_per_sample)(const GdkPixbuf *pixbuf);
guchar *(*fp_gdk_pixbuf_get_pixels)(const GdkPixbuf *pixbuf);
gboolean (*fp_gdk_pixbuf_get_has_alpha)(const GdkPixbuf *pixbuf);
int (*fp_gdk_pixbuf_get_height)(const GdkPixbuf *pixbuf);
int (*fp_gdk_pixbuf_get_n_channels)(const GdkPixbuf *pixbuf);
int (*fp_gdk_pixbuf_get_rowstride)(const GdkPixbuf *pixbuf);
int (*fp_gdk_pixbuf_get_width)(const GdkPixbuf *pixbuf);
GdkPixbuf *(*fp_gdk_pixbuf_new_from_file)(const char *filename, GError **error);
void (*fp_gtk_widget_destroy)(GtkWidget *widget);
void (*fp_gtk_window_present)(GtkWindow *window);
void (*fp_gtk_window_move)(GtkWindow *window, gint x, gint y);
void (*fp_gtk_window_resize)(GtkWindow *window, gint width, gint height);

/**
 * Function Pointers for GtkFileChooser
 */
gchar* (*fp_gtk_file_chooser_get_filename)(GtkFileChooser *chooser);
void (*fp_gtk_widget_hide)(GtkWidget *widget);
void (*fp_gtk_main_quit)(void);
GtkWidget* (*fp_gtk_file_chooser_dialog_new)(const gchar *title,
    GtkWindow *parent, GtkFileChooserAction action,
    const gchar *first_button_text, ...);
gboolean (*fp_gtk_file_chooser_set_current_folder)(GtkFileChooser *chooser,
    const gchar *filename);
gboolean (*fp_gtk_file_chooser_set_filename)(GtkFileChooser *chooser,
    const char *filename);
void (*fp_gtk_file_chooser_set_current_name)(GtkFileChooser *chooser,
    const gchar *name);
void (*fp_gtk_file_filter_add_custom)(GtkFileFilter *filter,
    GtkFileFilterFlags needed, GtkFileFilterFunc func, gpointer data,
    GDestroyNotify notify);
void (*fp_gtk_file_chooser_set_filter)(GtkFileChooser *chooser,
    GtkFileFilter *filter);
GType (*fp_gtk_file_chooser_get_type)(void);
GtkFileFilter* (*fp_gtk_file_filter_new)(void);
void (*fp_gtk_file_chooser_set_do_overwrite_confirmation)(
    GtkFileChooser *chooser, gboolean do_overwrite_confirmation);
void (*fp_gtk_file_chooser_set_select_multiple)(
    GtkFileChooser *chooser, gboolean select_multiple);
gchar* (*fp_gtk_file_chooser_get_current_folder)(GtkFileChooser *chooser);
GSList* (*fp_gtk_file_chooser_get_filenames)(GtkFileChooser *chooser);
guint (*fp_gtk_g_slist_length)(GSList *list);
gulong (*fp_g_signal_connect_data)(gpointer instance,
    const gchar *detailed_signal, GCallback c_handler, gpointer data,
    GClosureNotify destroy_data, GConnectFlags connect_flags);
void (*fp_gtk_widget_show)(GtkWidget *widget);
void (*fp_gtk_main)(void);
guint (*fp_gtk_main_level)(void);

/**
 * Returns :
 * NULL if the GTK+ library is compatible with the given version, or a string
 * describing the version mismatch.
 */
gchar* (*fp_gtk_check_version)(guint required_major, guint required_minor,
				      guint required_micro);

void (*fp_g_thread_init)(GThreadFunctions *vtable);
void (*fp_gdk_threads_init)(void);
void (*fp_gdk_threads_enter)(void);
void (*fp_gdk_threads_leave)(void);

/* Glib */
#define g_main_context_iteration (*fp_g_main_context_iteration)
#define g_value_init (*fp_g_value_init)
#define g_type_is_a (*fp_g_type_is_a)
#define g_value_get_boolean (*fp_g_value_get_boolean)
#define g_value_get_char (*fp_g_value_get_char)
#define g_value_get_uchar (*fp_g_value_get_uchar)
#define g_value_get_int (*fp_g_value_get_int)
#define g_value_get_uint (*fp_g_value_get_uint)
#define g_value_get_long (*fp_g_value_get_long)
#define g_value_get_ulong (*fp_g_value_get_ulong)
#define g_value_get_int64 (*fp_g_value_get_int64)
#define g_value_get_uint64 (*fp_g_value_get_uint64)
#define g_value_get_float (*fp_g_value_get_float)
#define g_value_get_double (*fp_g_value_get_double)
#define g_value_get_string (*fp_g_value_get_string)
#define g_value_get_enum (*fp_g_value_get_enum)
#define g_value_get_flags (*fp_g_value_get_flags)
#define g_value_get_param (*fp_g_value_get_param)
#define g_value_get_boxed (*fp_g_value_get_boxed)
#define g_value_get_object (*fp_g_value_get_object)
#define g_param_spec_int (*fp_g_param_spec_int)
#define g_object_get (*fp_g_object_get)
#define g_object_set (*fp_g_object_set)
#define g_thread_init (*fp_g_thread_init)
#define g_object_unref (*fp_g_object_unref)
#define g_free (*fp_g_free)
#define g_slist_length (*fp_gtk_g_slist_length)
#define g_signal_connect_data (*fp_g_signal_connect_data)

/* GDK */
#define gdk_pixmap_new (*fp_gdk_pixmap_new)
#define gdk_gc_new (*fp_gdk_gc_new)
#define gdk_rgb_gc_set_foreground (*fp_gdk_rgb_gc_set_foreground)
#define gdk_draw_rectangle (*fp_gdk_draw_rectangle)
#define gdk_pixbuf_get_from_drawable (*fp_gdk_pixbuf_get_from_drawable)
#define gdk_drawable_get_size (*fp_gdk_drawable_get_size)
#define gdk_threads_leave (*fp_gdk_threads_leave)
#define gdk_threads_init (*fp_gdk_threads_init)
#define gdk_threads_enter (*fp_gdk_threads_enter)

/************************
 * Gtk function pointers
 ************************/
#define gtk_init_check (*fp_gtk_init_check)

/* Painting */
#define gtk_paint_hline (*fp_gtk_paint_hline)
#define gtk_paint_vline (*fp_gtk_paint_vline)
#define gtk_paint_shadow (*fp_gtk_paint_shadow)
#define gtk_paint_arrow (*fp_gtk_paint_arrow)
#define gtk_paint_diamond (*fp_gtk_paint_diamond)
#define gtk_paint_box (*fp_gtk_paint_box)
#define gtk_paint_flat_box (*fp_gtk_paint_flat_box)
#define gtk_paint_check (*fp_gtk_paint_check)
#define gtk_paint_option (*fp_gtk_paint_option)
#define gtk_paint_box_gap (*fp_gtk_paint_box_gap)
#define gtk_paint_extension (*fp_gtk_paint_extension)
#define gtk_paint_focus (*fp_gtk_paint_focus)
#define gtk_paint_slider (*fp_gtk_paint_slider)
#define gtk_paint_handle (*fp_gtk_paint_handle)
#define gtk_paint_expander (*fp_gtk_paint_expander)
#define gtk_style_apply_default_background (*fp_gtk_style_apply_default_background)

/* Widget creation */
#define gtk_arrow_new (*fp_gtk_arrow_new)
#define gtk_button_new (*fp_gtk_button_new)
#define gtk_check_button_new (*fp_gtk_check_button_new)
#define gtk_check_menu_item_new (*fp_gtk_check_menu_item_new)
#define gtk_color_selection_dialog_new (*fp_gtk_color_selection_dialog_new)
#define gtk_combo_box_new (*fp_gtk_combo_box_new)
#define gtk_combo_box_entry_new (*fp_gtk_combo_box_entry_new)
#define gtk_entry_new (*fp_gtk_entry_new)
#define gtk_fixed_new (*fp_gtk_fixed_new)
#define gtk_handle_box_new (*fp_gtk_handle_box_new)
#define gtk_hpaned_new (*fp_gtk_hpaned_new)
#define gtk_vpaned_new (*fp_gtk_vpaned_new)
#define gtk_hscale_new (*fp_gtk_hscale_new)
#define gtk_vscale_new (*fp_gtk_vscale_new)
#define gtk_hscrollbar_new (*fp_gtk_hscrollbar_new)
#define gtk_vscrollbar_new (*fp_gtk_vscrollbar_new)
#define gtk_hseparator_new (*fp_gtk_hseparator_new)
#define gtk_vseparator_new (*fp_gtk_vseparator_new)
#define gtk_image_new (*fp_gtk_image_new)
#define gtk_label_new (*fp_gtk_label_new)
#define gtk_menu_new (*fp_gtk_menu_new)
#define gtk_menu_bar_new (*fp_gtk_menu_bar_new)
#define gtk_menu_item_new (*fp_gtk_menu_item_new)
#define gtk_notebook_new (*fp_gtk_notebook_new)
#define gtk_progress_bar_new (*fp_gtk_progress_bar_new)
#define gtk_progress_bar_set_orientation (*fp_gtk_progress_bar_set_orientation)
#define gtk_radio_button_new (*fp_gtk_radio_button_new)
#define gtk_radio_menu_item_new (*fp_gtk_radio_menu_item_new)
#define gtk_scrolled_window_new (*fp_gtk_scrolled_window_new)
#define gtk_separator_menu_item_new (*fp_gtk_separator_menu_item_new)
#define gtk_separator_tool_item_new (*fp_gtk_separator_tool_item_new)
#define gtk_text_view_new (*fp_gtk_text_view_new)
#define gtk_toggle_button_new (*fp_gtk_toggle_button_new)
#define gtk_toolbar_new (*fp_gtk_toolbar_new)
#define gtk_tree_view_new (*fp_gtk_tree_view_new)
#define gtk_viewport_new (*fp_gtk_viewport_new)
#define gtk_window_new (*fp_gtk_window_new)
#define gtk_dialog_new (*fp_gtk_dialog_new)
#define gtk_spin_button_new (*fp_gtk_spin_button_new)
#define gtk_frame_new (*fp_gtk_frame_new)

/* Other widget operations */
#define gtk_adjustment_new (*fp_gtk_adjustment_new)
#define gtk_container_add (*fp_gtk_container_add)
#define gtk_menu_shell_append (*fp_gtk_menu_shell_append)
#define gtk_menu_item_set_submenu (*fp_gtk_menu_item_set_submenu)
#define gtk_widget_realize (*fp_gtk_widget_realize)
#define gtk_widget_render_icon (*fp_gtk_widget_render_icon)
#define gtk_widget_set_name (*fp_gtk_widget_set_name)
#define gtk_widget_set_parent (*fp_gtk_widget_set_parent)
#define gtk_widget_set_direction (*fp_gtk_widget_set_direction)
#define gtk_widget_style_get (*fp_gtk_widget_style_get)
#define gtk_widget_class_install_style_property (*fp_gtk_widget_class_install_style_property)
#define gtk_widget_class_find_style_property (*fp_gtk_widget_class_find_style_property)
#define gtk_widget_style_get_property (*fp_gtk_widget_style_get_property)
#define pango_font_description_to_string (*fp_pango_font_description_to_string)
#define gtk_settings_get_default (*fp_gtk_settings_get_default)
#define gtk_widget_get_settings (*fp_gtk_widget_get_settings)
#define gtk_border_get_type (*fp_gtk_border_get_type)
#define gtk_arrow_set (*fp_gtk_arrow_set)
#define gtk_widget_size_request (*fp_gtk_widget_size_request)
#define gtk_range_get_adjustment (*fp_gtk_range_get_adjustment)
#define gtk_widget_destroy (*fp_gtk_widget_destroy)
#define gtk_window_present (*fp_gtk_window_present)
#define gtk_window_move (*fp_gtk_window_move)
#define gtk_window_resize (*fp_gtk_window_resize)
#define gtk_widget_show (*fp_gtk_widget_show)
#define gtk_widget_hide (*fp_gtk_widget_hide)
#define gtk_main (*fp_gtk_main)
#define gtk_main_level (*fp_gtk_main_level)
#define gtk_main_quit (*fp_gtk_main_quit)
#define gtk_check_version (*fp_gtk_check_version)
#define gtk_init_check (*fp_gtk_init_check)

/* GdkPixbuf */
#define gdk_pixbuf_get_bits_per_sample (*fp_gdk_pixbuf_get_bits_per_sample)
#define gdk_pixbuf_get_pixels (*fp_gdk_pixbuf_get_pixels)
#define gdk_pixbuf_get_has_alpha (*fp_gdk_pixbuf_get_has_alpha)
#define gdk_pixbuf_get_height (*fp_gdk_pixbuf_get_height)
#define gdk_pixbuf_get_n_channels (*fp_gdk_pixbuf_get_n_channels)
#define gdk_pixbuf_get_rowstride (*fp_gdk_pixbuf_get_rowstride)
#define gdk_pixbuf_get_width (*fp_gdk_pixbuf_get_width)
#define gdk_pixbuf_new_from_file (*fp_gdk_pixbuf_new_from_file)
#define gdk_pixbuf_new (*fp_gdk_pixbuf_new)

/* GtkFileChooser */
#define gtk_file_chooser_get_filename (*fp_gtk_file_chooser_get_filename)
#define gtk_file_chooser_dialog_new (*fp_gtk_file_chooser_dialog_new)
#define gtk_file_chooser_set_current_folder (*fp_gtk_file_chooser_set_current_folder)
#define gtk_file_chooser_set_filename (*fp_gtk_file_chooser_set_filename)
#define gtk_file_chooser_set_current_name (*fp_gtk_file_chooser_set_current_name)
#define gtk_file_chooser_set_filter (*fp_gtk_file_chooser_set_filter)
#define gtk_file_chooser_get_type (*fp_gtk_file_chooser_get_type)
#define gtk_file_chooser_set_do_overwrite_confirmation (*fp_gtk_file_chooser_set_do_overwrite_confirmation)
#define gtk_file_chooser_set_select_multiple (*fp_gtk_file_chooser_set_select_multiple)
#define gtk_file_chooser_get_current_folder (*fp_gtk_file_chooser_get_current_folder)
#define gtk_file_chooser_get_filenames (*fp_gtk_file_chooser_get_filenames)
#define gtk_file_filter_add_custom (*fp_gtk_file_filter_add_custom)
#define gtk_file_filter_new (*fp_gtk_file_filter_new)

#endif /* __GTK_FP_H__ */
