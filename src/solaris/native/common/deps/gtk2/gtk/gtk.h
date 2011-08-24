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
#ifndef __GTK_H__
#define __GTK_H__

#define _G_TYPE_CIC(ip, gt, ct)       ((ct*) ip)
#define G_TYPE_CHECK_INSTANCE_CAST(instance, g_type, c_type)    (_G_TYPE_CIC ((instance), (g_type), c_type))
#define GTK_TYPE_FILE_CHOOSER             (fp_gtk_file_chooser_get_type ())
#define GTK_FILE_CHOOSER(obj) (G_TYPE_CHECK_INSTANCE_CAST ((obj), GTK_TYPE_FILE_CHOOSER, GtkFileChooser))
#define fp_g_signal_connect(instance, detailed_signal, c_handler, data) \
    fp_g_signal_connect_data ((instance), (detailed_signal), (c_handler), (data), NULL, (GConnectFlags) 0)
#define G_CALLBACK(f) ((GCallback) (f))
#define G_TYPE_FUNDAMENTAL_SHIFT (2)
#define G_TYPE_MAKE_FUNDAMENTAL(x) ((GType) ((x) << G_TYPE_FUNDAMENTAL_SHIFT))
#define G_TYPE_OBJECT G_TYPE_MAKE_FUNDAMENTAL (20)
#define G_OBJECT(object) (G_TYPE_CHECK_INSTANCE_CAST ((object), G_TYPE_OBJECT, GObject))
#define GTK_STOCK_CANCEL           "gtk-cancel"
#define GTK_STOCK_SAVE             "gtk-save"
#define GTK_STOCK_OPEN             "gtk-open"

/* GTK types, here to eliminate need for GTK headers at compile time */

#ifndef FALSE
#define FALSE           (0)
#define TRUE            (!FALSE)
#endif

#define GTK_HAS_FOCUS   (1 << 12)
#define GTK_HAS_DEFAULT (1 << 14)


/* basic types */
typedef char    gchar;
typedef short   gshort;
typedef int     gint;
typedef long    glong;
typedef float   gfloat;
typedef double  gdouble;
typedef void*   gpointer;
typedef gint    gboolean;

typedef signed char  gint8;
typedef signed short gint16;
typedef signed int   gint32;

typedef unsigned char  guchar;
typedef unsigned char  guint8;
typedef unsigned short gushort;
typedef unsigned short guint16;
typedef unsigned int   guint;
typedef unsigned int   guint32;
typedef unsigned int   gsize;
typedef unsigned long  gulong;

typedef signed long long   gint64;
typedef unsigned long long guint64;

/* enumerated constants */
typedef enum
{
  GTK_ARROW_UP,
  GTK_ARROW_DOWN,
  GTK_ARROW_LEFT,
  GTK_ARROW_RIGHT
} GtkArrowType;

typedef enum {
  GDK_COLORSPACE_RGB
} GdkColorspace;

typedef enum
{
  GTK_EXPANDER_COLLAPSED,
  GTK_EXPANDER_SEMI_COLLAPSED,
  GTK_EXPANDER_SEMI_EXPANDED,
  GTK_EXPANDER_EXPANDED
} GtkExpanderStyle;

typedef enum
{
  GTK_ICON_SIZE_INVALID,
  GTK_ICON_SIZE_MENU,
  GTK_ICON_SIZE_SMALL_TOOLBAR,
  GTK_ICON_SIZE_LARGE_TOOLBAR,
  GTK_ICON_SIZE_BUTTON,
  GTK_ICON_SIZE_DND,
  GTK_ICON_SIZE_DIALOG
} GtkIconSize;

typedef enum
{
  GTK_ORIENTATION_HORIZONTAL,
  GTK_ORIENTATION_VERTICAL
} GtkOrientation;

typedef enum
{
  GTK_POS_LEFT,
  GTK_POS_RIGHT,
  GTK_POS_TOP,
  GTK_POS_BOTTOM
} GtkPositionType;

typedef enum
{
  GTK_SHADOW_NONE,
  GTK_SHADOW_IN,
  GTK_SHADOW_OUT,
  GTK_SHADOW_ETCHED_IN,
  GTK_SHADOW_ETCHED_OUT
} GtkShadowType;

typedef enum
{
  GTK_STATE_NORMAL,
  GTK_STATE_ACTIVE,
  GTK_STATE_PRELIGHT,
  GTK_STATE_SELECTED,
  GTK_STATE_INSENSITIVE
} GtkStateType;

typedef enum
{
  GTK_TEXT_DIR_NONE,
  GTK_TEXT_DIR_LTR,
  GTK_TEXT_DIR_RTL
} GtkTextDirection;

typedef enum
{
  GTK_WINDOW_TOPLEVEL,
  GTK_WINDOW_POPUP
} GtkWindowType;

typedef enum
{
  G_PARAM_READABLE            = 1 << 0,
  G_PARAM_WRITABLE            = 1 << 1,
  G_PARAM_CONSTRUCT           = 1 << 2,
  G_PARAM_CONSTRUCT_ONLY      = 1 << 3,
  G_PARAM_LAX_VALIDATION      = 1 << 4,
  G_PARAM_PRIVATE             = 1 << 5
} GParamFlags;

/* We define all structure pointers to be void* */
typedef void GError;
typedef void GMainContext;

typedef struct _GSList GSList;
struct _GSList
{
  gpointer data;
  GSList *next;
};

typedef void GdkColormap;
typedef void GdkDrawable;
typedef void GdkGC;
typedef void GdkPixbuf;
typedef void GdkPixmap;
typedef void GdkWindow;

typedef void GtkFixed;
typedef void GtkMenuItem;
typedef void GtkMenuShell;
typedef void GtkWidgetClass;
typedef void PangoFontDescription;
typedef void GtkSettings;

/* Some real structures */
typedef struct
{
  guint32 pixel;
  guint16 red;
  guint16 green;
  guint16 blue;
} GdkColor;

typedef struct {
  gint      fd;
  gushort   events;
  gushort   revents;
} GPollFD;

typedef struct {
  gint x;
  gint y;
  gint width;
  gint height;
} GdkRectangle;

typedef struct {
  gint x;
  gint y;
  gint width;
  gint height;
} GtkAllocation;

typedef struct {
  gint width;
  gint height;
} GtkRequisition;

typedef struct {
  GtkWidgetClass *g_class;
} GTypeInstance;

typedef struct {
  gint left;
  gint right;
  gint top;
  gint bottom;
} GtkBorder;

/******************************************************
 * FIXME: it is more safe to include gtk headers for
 * the precise type definition of GType and other
 * structures. This is a place where getting rid of gtk
 * headers may be dangerous.
 ******************************************************/
typedef gulong         GType;

typedef struct
{
  GType         g_type;

  union {
    gint        v_int;
    guint       v_uint;
    glong       v_long;
    gulong      v_ulong;
    gint64      v_int64;
    guint64     v_uint64;
    gfloat      v_float;
    gdouble     v_double;
    gpointer    v_pointer;
  } data[2];
} GValue;

typedef struct
{
  GTypeInstance  g_type_instance;

  gchar         *name;
  GParamFlags    flags;
  GType          value_type;
  GType          owner_type;
} GParamSpec;

typedef struct {
  GTypeInstance g_type_instance;
  guint         ref_count;
  void         *qdata;
} GObject;

typedef struct {
  GObject parent_instance;
  guint32 flags;
} GtkObject;

typedef struct
{
  GObject parent_instance;

  GdkColor fg[5];
  GdkColor bg[5];
  GdkColor light[5];
  GdkColor dark[5];
  GdkColor mid[5];
  GdkColor text[5];
  GdkColor base[5];
  GdkColor text_aa[5];          /* Halfway between text/base */

  GdkColor black;
  GdkColor white;
  PangoFontDescription *font_desc;

  gint xthickness;
  gint ythickness;

  GdkGC *fg_gc[5];
  GdkGC *bg_gc[5];
  GdkGC *light_gc[5];
  GdkGC *dark_gc[5];
  GdkGC *mid_gc[5];
  GdkGC *text_gc[5];
  GdkGC *base_gc[5];
  GdkGC *text_aa_gc[5];
  GdkGC *black_gc;
  GdkGC *white_gc;

  GdkPixmap *bg_pixmap[5];
} GtkStyle;

typedef struct _GtkWidget GtkWidget;
struct _GtkWidget
{
  GtkObject object;
  guint16 private_flags;
  guint8 state;
  guint8 saved_state;
  gchar *name;
  GtkStyle *style;
  GtkRequisition requisition;
  GtkAllocation allocation;
  GdkWindow *window;
  GtkWidget *parent;
};

typedef struct
{
  GtkWidget widget;

  gfloat xalign;
  gfloat yalign;

  guint16 xpad;
  guint16 ypad;
} GtkMisc;

typedef struct {
  GtkWidget widget;
  GtkWidget *focus_child;
  guint border_width : 16;
  guint need_resize : 1;
  guint resize_mode : 2;
  guint reallocate_redraws : 1;
  guint has_focus_chain : 1;
} GtkContainer;

typedef struct {
  GtkContainer container;
  GtkWidget *child;
} GtkBin;

typedef struct {
  GtkBin bin;
  GdkWindow *event_window;
  gchar *label_text;
  guint activate_timeout;
  guint constructed : 1;
  guint in_button : 1;
  guint button_down : 1;
  guint relief : 2;
  guint use_underline : 1;
  guint use_stock : 1;
  guint depressed : 1;
  guint depress_on_activate : 1;
  guint focus_on_click : 1;
} GtkButton;

typedef struct {
  GtkButton button;
  guint active : 1;
  guint draw_indicator : 1;
  guint inconsistent : 1;
} GtkToggleButton;

typedef struct _GtkAdjustment GtkAdjustment;
struct _GtkAdjustment
{
  GtkObject parent_instance;

  gdouble lower;
  gdouble upper;
  gdouble value;
  gdouble step_increment;
  gdouble page_increment;
  gdouble page_size;
};

typedef enum
{
  GTK_UPDATE_CONTINUOUS,
  GTK_UPDATE_DISCONTINUOUS,
  GTK_UPDATE_DELAYED
} GtkUpdateType;

typedef struct _GtkRange GtkRange;
struct _GtkRange
{
  GtkWidget widget;
  GtkAdjustment *adjustment;
  GtkUpdateType update_policy;
  guint inverted : 1;
  /*< protected >*/
  guint flippable : 1;
  guint has_stepper_a : 1;
  guint has_stepper_b : 1;
  guint has_stepper_c : 1;
  guint has_stepper_d : 1;
  guint need_recalc : 1;
  guint slider_size_fixed : 1;
  gint min_slider_size;
  GtkOrientation orientation;
  GdkRectangle range_rect;
  gint slider_start, slider_end;
  gint round_digits;
  /*< private >*/
  guint trough_click_forward : 1;
  guint update_pending : 1;
  /*GtkRangeLayout * */ void *layout;
  /*GtkRangeStepTimer * */ void* timer;
  gint slide_initial_slider_position;
  gint slide_initial_coordinate;
  guint update_timeout_id;
  GdkWindow *event_window;
};

typedef struct _GtkProgressBar       GtkProgressBar;

typedef enum
{
  GTK_PROGRESS_CONTINUOUS,
  GTK_PROGRESS_DISCRETE
} GtkProgressBarStyle;

typedef enum
{
  GTK_PROGRESS_LEFT_TO_RIGHT,
  GTK_PROGRESS_RIGHT_TO_LEFT,
  GTK_PROGRESS_BOTTOM_TO_TOP,
  GTK_PROGRESS_TOP_TO_BOTTOM
} GtkProgressBarOrientation;

typedef struct _GtkProgress       GtkProgress;

struct _GtkProgress
{
  GtkWidget widget;
  GtkAdjustment *adjustment;
  GdkPixmap     *offscreen_pixmap;
  gchar         *format;
  gfloat         x_align;
  gfloat         y_align;
  guint          show_text : 1;
  guint          activity_mode : 1;
  guint          use_text_format : 1;
};

struct _GtkProgressBar
{
  GtkProgress progress;
  GtkProgressBarStyle bar_style;
  GtkProgressBarOrientation orientation;
  guint blocks;
  gint  in_block;
  gint  activity_pos;
  guint activity_step;
  guint activity_blocks;
  gdouble pulse_fraction;
  guint activity_dir : 1;
  guint ellipsize : 3;
};

typedef enum {
  GTK_RESPONSE_NONE = -1,
  GTK_RESPONSE_REJECT = -2,
  GTK_RESPONSE_ACCEPT = -3,
  GTK_RESPONSE_DELETE_EVENT = -4,
  GTK_RESPONSE_OK = -5,
  GTK_RESPONSE_CANCEL = -6,
  GTK_RESPONSE_CLOSE = -7,
  GTK_RESPONSE_YES = -8,
  GTK_RESPONSE_NO = -9,
  GTK_RESPONSE_APPLY = -10,
  GTK_RESPONSE_HELP = -11
} GtkResponseType;

typedef struct _GtkWindow GtkWindow;

typedef struct _GtkFileChooser GtkFileChooser;

typedef enum {
  GTK_FILE_CHOOSER_ACTION_OPEN,
  GTK_FILE_CHOOSER_ACTION_SAVE,
  GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER,
  GTK_FILE_CHOOSER_ACTION_CREATE_FOLDER
} GtkFileChooserAction;

typedef struct _GtkFileFilter GtkFileFilter;

typedef enum {
  GTK_FILE_FILTER_FILENAME = 1 << 0,
  GTK_FILE_FILTER_URI = 1 << 1,
  GTK_FILE_FILTER_DISPLAY_NAME = 1 << 2,
  GTK_FILE_FILTER_MIME_TYPE = 1 << 3
} GtkFileFilterFlags;

typedef struct {
  GtkFileFilterFlags contains;
  const gchar *filename;
  const gchar *uri;
  const gchar *display_name;
  const gchar *mime_type;
} GtkFileFilterInfo;

typedef gboolean (*GtkFileFilterFunc)(const GtkFileFilterInfo *filter_info,
    gpointer data);

typedef void (*GDestroyNotify)(gpointer data);

typedef void (*GCallback)(void);

typedef struct _GClosure GClosure;

typedef void (*GClosureNotify)(gpointer data, GClosure *closure);

typedef enum {
  G_CONNECT_AFTER = 1 << 0, G_CONNECT_SWAPPED = 1 << 1
} GConnectFlags;

typedef struct _GThreadFunctions GThreadFunctions;

#define G_TYPE_INVALID                  G_TYPE_MAKE_FUNDAMENTAL (0)
#define G_TYPE_NONE                     G_TYPE_MAKE_FUNDAMENTAL (1)
#define G_TYPE_INTERFACE                G_TYPE_MAKE_FUNDAMENTAL (2)
#define G_TYPE_CHAR                     G_TYPE_MAKE_FUNDAMENTAL (3)
#define G_TYPE_UCHAR                    G_TYPE_MAKE_FUNDAMENTAL (4)
#define G_TYPE_BOOLEAN                  G_TYPE_MAKE_FUNDAMENTAL (5)
#define G_TYPE_INT                      G_TYPE_MAKE_FUNDAMENTAL (6)
#define G_TYPE_UINT                     G_TYPE_MAKE_FUNDAMENTAL (7)
#define G_TYPE_LONG                     G_TYPE_MAKE_FUNDAMENTAL (8)
#define G_TYPE_ULONG                    G_TYPE_MAKE_FUNDAMENTAL (9)
#define G_TYPE_INT64                    G_TYPE_MAKE_FUNDAMENTAL (10)
#define G_TYPE_UINT64                   G_TYPE_MAKE_FUNDAMENTAL (11)
#define G_TYPE_ENUM                     G_TYPE_MAKE_FUNDAMENTAL (12)
#define G_TYPE_FLAGS                    G_TYPE_MAKE_FUNDAMENTAL (13)
#define G_TYPE_FLOAT                    G_TYPE_MAKE_FUNDAMENTAL (14)
#define G_TYPE_DOUBLE                   G_TYPE_MAKE_FUNDAMENTAL (15)
#define G_TYPE_STRING                   G_TYPE_MAKE_FUNDAMENTAL (16)
#define G_TYPE_POINTER                  G_TYPE_MAKE_FUNDAMENTAL (17)
#define G_TYPE_BOXED                    G_TYPE_MAKE_FUNDAMENTAL (18)
#define G_TYPE_PARAM                    G_TYPE_MAKE_FUNDAMENTAL (19)
#define G_TYPE_OBJECT                   G_TYPE_MAKE_FUNDAMENTAL (20)

#define GTK_TYPE_BORDER                 gtk_border_get_type()

#define G_TYPE_FUNDAMENTAL_SHIFT        (2)
#define G_TYPE_MAKE_FUNDAMENTAL(x)      ((GType) ((x) << G_TYPE_FUNDAMENTAL_SHIFT))

#define g_signal_connect(instance, detailed_signal, c_handler, data) \
 g_signal_connect_data ((instance), (detailed_signal), (c_handler), (data), NULL, (GConnectFlags) 0)

#endif /* __GTK_H__ */
