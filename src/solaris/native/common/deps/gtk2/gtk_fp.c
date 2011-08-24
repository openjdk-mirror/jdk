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
#include <dlfcn.h>
#include <setjmp.h>
#include <stddef.h>

#include "gtk_fp.h"

#define GTK2_LIB "libgtk-x11-2.0.so.0"
#define GTHREAD_LIB "libgthread-2.0.so.0"
#define NO_SYMBOL_EXCEPTION 1

static gboolean new_combo_used = TRUE;
static void *gtk2_libhandle = NULL;
static void *gthread_libhandle = NULL;
static jmp_buf j;

/* This is a workaround for the bug:
 * http://sourceware.org/bugzilla/show_bug.cgi?id=1814
 * (dlsym/dlopen clears dlerror state)
 * This bug is specific to Linux, but there is no harm in
 * applying this workaround on Solaris as well.
 */
static void* dl_symbol(const char* name)
{
    void* result = dlsym(gtk2_libhandle, name);
    if (!result)
        longjmp(j, NO_SYMBOL_EXCEPTION);

    return result;
}

static void* dl_symbol_gthread(const char* name)
{
    void* result = dlsym(gthread_libhandle, name);
    if (!result)
        longjmp(j, NO_SYMBOL_EXCEPTION);

    return result;
}

gboolean gtk2_check_dlversion()
{
    if (gtk2_libhandle != NULL) {
        /* We've already successfully opened the GTK libs, so return true. */
        return TRUE;
    }
    void *lib = NULL;
    gboolean result = FALSE;
    
    lib = dlopen(GTK2_LIB, RTLD_LAZY | RTLD_LOCAL);
    if (lib == NULL) {
      return FALSE;
    }
    
    fp_gtk_check_version = dlsym(lib, "gtk_check_version");
    /* Check for GTK 2.2+ */
    if (!fp_gtk_check_version(2, 2, 0)) {
      result = TRUE;
    }
    
    dlclose(lib);
    
    return result;
}

/**
 * Functions for sun_awt_X11_GtkFileDialogPeer.c
 */
void gtk2_file_chooser_load()
{
    fp_gtk_file_chooser_get_filename = dl_symbol(
            "gtk_file_chooser_get_filename");
    fp_gtk_file_chooser_dialog_new = dl_symbol("gtk_file_chooser_dialog_new");
    fp_gtk_file_chooser_set_current_folder = dl_symbol(
            "gtk_file_chooser_set_current_folder");
    fp_gtk_file_chooser_set_filename = dl_symbol(
            "gtk_file_chooser_set_filename");
    fp_gtk_file_chooser_set_current_name = dl_symbol(
            "gtk_file_chooser_set_current_name");
    fp_gtk_file_filter_add_custom = dl_symbol("gtk_file_filter_add_custom");
    fp_gtk_file_chooser_set_filter = dl_symbol("gtk_file_chooser_set_filter");
    fp_gtk_file_chooser_get_type = dl_symbol("gtk_file_chooser_get_type");
    fp_gtk_file_filter_new = dl_symbol("gtk_file_filter_new");
    if (fp_gtk_check_version(2, 8, 0) == NULL) {
        fp_gtk_file_chooser_set_do_overwrite_confirmation = dl_symbol(
                "gtk_file_chooser_set_do_overwrite_confirmation");
    }
    fp_gtk_file_chooser_set_select_multiple = dl_symbol(
            "gtk_file_chooser_set_select_multiple");
    fp_gtk_file_chooser_get_current_folder = dl_symbol(
            "gtk_file_chooser_get_current_folder");
    fp_gtk_file_chooser_get_filenames = dl_symbol(
            "gtk_file_chooser_get_filenames");
    fp_gtk_g_slist_length = dl_symbol("g_slist_length");
}

gboolean gtk2_dlload()
{
    gtk2_libhandle = dlopen(GTK2_LIB, RTLD_LAZY | RTLD_LOCAL);
    gthread_libhandle = dlopen(GTHREAD_LIB, RTLD_LAZY | RTLD_LOCAL);

    if (gtk2_libhandle == NULL || gthread_libhandle == NULL)
        return FALSE;

    if (setjmp(j) == 0)
    {
        fp_gtk_check_version = dl_symbol("gtk_check_version");
        /* Check for GTK 2.2+ */
        if (fp_gtk_check_version(2, 2, 0)) {
            longjmp(j, NO_SYMBOL_EXCEPTION);
        }

        /* GLib */
        fp_g_free = dl_symbol("g_free");
        fp_g_object_unref = dl_symbol("g_object_unref");

        fp_g_main_context_iteration =
            dl_symbol("g_main_context_iteration");

        fp_g_value_init = dl_symbol("g_value_init");
        fp_g_type_is_a = dl_symbol("g_type_is_a");

        fp_g_value_get_boolean = dl_symbol("g_value_get_boolean");
        fp_g_value_get_char = dl_symbol("g_value_get_char");
        fp_g_value_get_uchar = dl_symbol("g_value_get_uchar");
        fp_g_value_get_int = dl_symbol("g_value_get_int");
        fp_g_value_get_uint = dl_symbol("g_value_get_uint");
        fp_g_value_get_long = dl_symbol("g_value_get_long");
        fp_g_value_get_ulong = dl_symbol("g_value_get_ulong");
        fp_g_value_get_int64 = dl_symbol("g_value_get_int64");
        fp_g_value_get_uint64 = dl_symbol("g_value_get_uint64");
        fp_g_value_get_float = dl_symbol("g_value_get_float");
        fp_g_value_get_double = dl_symbol("g_value_get_double");
        fp_g_value_get_string = dl_symbol("g_value_get_string");
        fp_g_value_get_enum = dl_symbol("g_value_get_enum");
        fp_g_value_get_flags = dl_symbol("g_value_get_flags");
        fp_g_value_get_param = dl_symbol("g_value_get_param");
        fp_g_value_get_boxed = dl_symbol("g_value_get_boxed");
        fp_g_value_get_pointer = dl_symbol("g_value_get_pointer");
        fp_g_value_get_object = dl_symbol("g_value_get_object");
        fp_g_param_spec_int = dl_symbol("g_param_spec_int");
        fp_g_object_get = dl_symbol("g_object_get");
        fp_g_object_set = dl_symbol("g_object_set");

        /* GDK */
        fp_gdk_pixmap_new = dl_symbol("gdk_pixmap_new");
        fp_gdk_pixbuf_get_from_drawable =
            dl_symbol("gdk_pixbuf_get_from_drawable");
        fp_gdk_gc_new = dl_symbol("gdk_gc_new");
        fp_gdk_rgb_gc_set_foreground =
            dl_symbol("gdk_rgb_gc_set_foreground");
        fp_gdk_draw_rectangle = dl_symbol("gdk_draw_rectangle");
        fp_gdk_drawable_get_size = dl_symbol("gdk_drawable_get_size");

        /* Pixbuf */
        fp_gdk_pixbuf_new = dl_symbol("gdk_pixbuf_new");
        fp_gdk_pixbuf_new_from_file =
                dl_symbol("gdk_pixbuf_new_from_file");
        fp_gdk_pixbuf_get_width = dl_symbol("gdk_pixbuf_get_width");
        fp_gdk_pixbuf_get_height = dl_symbol("gdk_pixbuf_get_height");
        fp_gdk_pixbuf_get_pixels = dl_symbol("gdk_pixbuf_get_pixels");
        fp_gdk_pixbuf_get_rowstride =
                dl_symbol("gdk_pixbuf_get_rowstride");
        fp_gdk_pixbuf_get_has_alpha =
                dl_symbol("gdk_pixbuf_get_has_alpha");
        fp_gdk_pixbuf_get_bits_per_sample =
                dl_symbol("gdk_pixbuf_get_bits_per_sample");
        fp_gdk_pixbuf_get_n_channels =
                dl_symbol("gdk_pixbuf_get_n_channels");

        /* GTK painting */
        fp_gtk_init_check = dl_symbol("gtk_init_check");
        fp_gtk_paint_hline = dl_symbol("gtk_paint_hline");
        fp_gtk_paint_vline = dl_symbol("gtk_paint_vline");
        fp_gtk_paint_shadow = dl_symbol("gtk_paint_shadow");
        fp_gtk_paint_arrow = dl_symbol("gtk_paint_arrow");
        fp_gtk_paint_diamond = dl_symbol("gtk_paint_diamond");
        fp_gtk_paint_box = dl_symbol("gtk_paint_box");
        fp_gtk_paint_flat_box = dl_symbol("gtk_paint_flat_box");
        fp_gtk_paint_check = dl_symbol("gtk_paint_check");
        fp_gtk_paint_option = dl_symbol("gtk_paint_option");
        fp_gtk_paint_box_gap = dl_symbol("gtk_paint_box_gap");
        fp_gtk_paint_extension = dl_symbol("gtk_paint_extension");
        fp_gtk_paint_focus = dl_symbol("gtk_paint_focus");
        fp_gtk_paint_slider = dl_symbol("gtk_paint_slider");
        fp_gtk_paint_handle = dl_symbol("gtk_paint_handle");
        fp_gtk_paint_expander = dl_symbol("gtk_paint_expander");
        fp_gtk_style_apply_default_background =
                dl_symbol("gtk_style_apply_default_background");

        /* GTK widgets */
        fp_gtk_arrow_new = dl_symbol("gtk_arrow_new");
        fp_gtk_button_new = dl_symbol("gtk_button_new");
        fp_gtk_spin_button_new = dl_symbol("gtk_spin_button_new");
        fp_gtk_check_button_new = dl_symbol("gtk_check_button_new");
        fp_gtk_check_menu_item_new =
                dl_symbol("gtk_check_menu_item_new");
        fp_gtk_color_selection_dialog_new =
                dl_symbol("gtk_color_selection_dialog_new");
        fp_gtk_entry_new = dl_symbol("gtk_entry_new");
        fp_gtk_fixed_new = dl_symbol("gtk_fixed_new");
        fp_gtk_handle_box_new = dl_symbol("gtk_handle_box_new");
        fp_gtk_image_new = dl_symbol("gtk_image_new");
        fp_gtk_hpaned_new = dl_symbol("gtk_hpaned_new");
        fp_gtk_vpaned_new = dl_symbol("gtk_vpaned_new");
        fp_gtk_hscale_new = dl_symbol("gtk_hscale_new");
        fp_gtk_vscale_new = dl_symbol("gtk_vscale_new");
        fp_gtk_hscrollbar_new = dl_symbol("gtk_hscrollbar_new");
        fp_gtk_vscrollbar_new = dl_symbol("gtk_vscrollbar_new");
        fp_gtk_hseparator_new = dl_symbol("gtk_hseparator_new");
        fp_gtk_vseparator_new = dl_symbol("gtk_vseparator_new");
        fp_gtk_label_new = dl_symbol("gtk_label_new");
        fp_gtk_menu_new = dl_symbol("gtk_menu_new");
        fp_gtk_menu_bar_new = dl_symbol("gtk_menu_bar_new");
        fp_gtk_menu_item_new = dl_symbol("gtk_menu_item_new");
        fp_gtk_menu_item_set_submenu =
                dl_symbol("gtk_menu_item_set_submenu");
        fp_gtk_notebook_new = dl_symbol("gtk_notebook_new");
        fp_gtk_progress_bar_new =
            dl_symbol("gtk_progress_bar_new");
        fp_gtk_progress_bar_set_orientation =
            dl_symbol("gtk_progress_bar_set_orientation");
        fp_gtk_radio_button_new =
            dl_symbol("gtk_radio_button_new");
        fp_gtk_radio_menu_item_new =
            dl_symbol("gtk_radio_menu_item_new");
        fp_gtk_scrolled_window_new =
            dl_symbol("gtk_scrolled_window_new");
        fp_gtk_separator_menu_item_new =
            dl_symbol("gtk_separator_menu_item_new");
        fp_gtk_text_view_new = dl_symbol("gtk_text_view_new");
        fp_gtk_toggle_button_new =
            dl_symbol("gtk_toggle_button_new");
        fp_gtk_toolbar_new = dl_symbol("gtk_toolbar_new");
        fp_gtk_tree_view_new = dl_symbol("gtk_tree_view_new");
        fp_gtk_viewport_new = dl_symbol("gtk_viewport_new");
        fp_gtk_window_new = dl_symbol("gtk_window_new");
        fp_gtk_window_present = dl_symbol("gtk_window_present");
        fp_gtk_window_move = dl_symbol("gtk_window_move");
        fp_gtk_window_resize = dl_symbol("gtk_window_resize");

          fp_gtk_dialog_new = dl_symbol("gtk_dialog_new");
        fp_gtk_frame_new = dl_symbol("gtk_frame_new");

        fp_gtk_adjustment_new = dl_symbol("gtk_adjustment_new");
        fp_gtk_container_add = dl_symbol("gtk_container_add");
        fp_gtk_menu_shell_append =
            dl_symbol("gtk_menu_shell_append");
        fp_gtk_widget_realize = dl_symbol("gtk_widget_realize");
        fp_gtk_widget_destroy = dl_symbol("gtk_widget_destroy");
        fp_gtk_widget_render_icon =
            dl_symbol("gtk_widget_render_icon");
        fp_gtk_widget_set_name =
            dl_symbol("gtk_widget_set_name");
        fp_gtk_widget_set_parent =
            dl_symbol("gtk_widget_set_parent");
        fp_gtk_widget_set_direction =
            dl_symbol("gtk_widget_set_direction");
        fp_gtk_widget_style_get =
            dl_symbol("gtk_widget_style_get");
        fp_gtk_widget_class_install_style_property =
            dl_symbol("gtk_widget_class_install_style_property");
        fp_gtk_widget_class_find_style_property =
            dl_symbol("gtk_widget_class_find_style_property");
        fp_gtk_widget_style_get_property =
            dl_symbol("gtk_widget_style_get_property");
        fp_pango_font_description_to_string =
            dl_symbol("pango_font_description_to_string");
        fp_gtk_settings_get_default =
            dl_symbol("gtk_settings_get_default");
        fp_gtk_widget_get_settings =
            dl_symbol("gtk_widget_get_settings");
        fp_gtk_border_get_type =  dl_symbol("gtk_border_get_type");
        fp_gtk_arrow_set = dl_symbol("gtk_arrow_set");
        fp_gtk_widget_size_request =
            dl_symbol("gtk_widget_size_request");
        fp_gtk_range_get_adjustment =
            dl_symbol("gtk_range_get_adjustment");

        fp_gtk_widget_hide = dl_symbol("gtk_widget_hide");
        fp_gtk_main_quit = dl_symbol("gtk_main_quit");
        fp_g_signal_connect_data = dl_symbol("g_signal_connect_data");
        fp_gtk_widget_show = dl_symbol("gtk_widget_show");
        fp_gtk_main = dl_symbol("gtk_main");

        /**
         * GLib thread system
         */
        fp_g_thread_init = dl_symbol_gthread("g_thread_init");
        fp_gdk_threads_init = dl_symbol("gdk_threads_init");
        fp_gdk_threads_enter = dl_symbol("gdk_threads_enter");
        fp_gdk_threads_leave = dl_symbol("gdk_threads_leave");

        /**
         * Functions for sun_awt_X11_GtkFileDialogPeer.c
         */
        if (fp_gtk_check_version(2, 4, 0) == NULL) {
            // The current GtkFileChooser is available from GTK+ 2.4
            gtk2_file_chooser_load();
        }

        /* Some functions may be missing in pre-2.4 GTK.
           We handle them specially here.
         */
        fp_gtk_combo_box_new = dlsym(gtk2_libhandle, "gtk_combo_box_new");
        if (fp_gtk_combo_box_new == NULL) {
            fp_gtk_combo_box_new = dl_symbol("gtk_combo_new");
        }

        fp_gtk_combo_box_entry_new =
            dlsym(gtk2_libhandle, "gtk_combo_box_entry_new");
        if (fp_gtk_combo_box_entry_new == NULL) {
            fp_gtk_combo_box_entry_new = dl_symbol("gtk_combo_new");
            new_combo_used = FALSE;
        }

        fp_gtk_separator_tool_item_new =
            dlsym(gtk2_libhandle, "gtk_separator_tool_item_new");
        if (fp_gtk_separator_tool_item_new == NULL) {
            fp_gtk_separator_tool_item_new =
                dl_symbol("gtk_vseparator_new");
        }
    }
    /* Now we have only one kind of exceptions: NO_SYMBOL_EXCEPTION
     * Otherwise we can check the return value of setjmp method.
     */
    else
    {
        dlclose(gtk2_libhandle);
        gtk2_libhandle = NULL;

        dlclose(gthread_libhandle);
        gthread_libhandle = NULL;

        return FALSE;
    }

    return TRUE;
}

int gtk2_dlunload()
{
    char *gtk2_error;

    if (!gtk2_libhandle)
        return TRUE;

    dlerror();
    dlclose(gtk2_libhandle);
    dlclose(gthread_libhandle);
    if ((gtk2_error = dlerror()) != NULL)
    {
        return FALSE;
    }
    return TRUE;
}

gboolean new_combo()
{
  return new_combo_used;
}

