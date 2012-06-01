/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>

#include "java_awt_SystemColor.h"

#include <Beep.h>
#include <dlfcn.h>
#include <kernel/OS.h>

#include "AwtApplication.h"
#include "Utilities.h"

static uint32 RgbColorToInt(rgb_color color) {
	return ((color.alpha & 0xFF) << 24)
		| ((color.red & 0xFF) << 16)
		| ((color.green & 0xFF) << 8)
		| ((color.blue & 0xFF));
}

extern "C" {

JavaVM* jvm;

/*
 * Class:     sun_hawt_HaikuToolkit
 * Method:    nativeRunMessage
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuToolkit_nativeRunMessage(JNIEnv *env, jobject thiz)
{
    BApplication* awtApp = new AwtApplication("application/x-vnd.java-awt-app");
    CreatedBeApp();
    awtApp->Run();
    delete awtApp;
}

/*
 * Class:     sun_hawt_HaikuToolkit
 * Method:    nativeShutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuToolkit_nativeShutdown(JNIEnv *env, jobject thiz)
{
    WaitForBeApp();
    be_app->LockLooper();
    be_app->Quit();
    be_app->UnlockLooper();
}

/*
 * Class:     sun_hawt_HaikuToolkit
 * Method:    nativeLoadSystemColors
 * Signature: ([I)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuToolkit_nativeLoadSystemColors(JNIEnv *env, jobject thiz,
	jintArray systemColors)
{
    jint* colors = env->GetIntArrayElements(systemColors, NULL);
    if (colors == NULL)
    	return;
    
    colors[java_awt_SystemColor_DESKTOP]                 = RgbColorToInt(ui_color(B_DESKTOP_COLOR));
    colors[java_awt_SystemColor_ACTIVE_CAPTION]          = RgbColorToInt(ui_color(B_WINDOW_TAB_COLOR));
    colors[java_awt_SystemColor_ACTIVE_CAPTION_TEXT]     = RgbColorToInt(ui_color(B_WINDOW_TEXT_COLOR));
    colors[java_awt_SystemColor_ACTIVE_CAPTION_BORDER]   = RgbColorToInt(ui_color(B_WINDOW_TAB_COLOR));
    colors[java_awt_SystemColor_INACTIVE_CAPTION]        = RgbColorToInt(ui_color(B_WINDOW_INACTIVE_TAB_COLOR));
    colors[java_awt_SystemColor_INACTIVE_CAPTION_TEXT]   = RgbColorToInt(ui_color(B_WINDOW_INACTIVE_TEXT_COLOR));
    colors[java_awt_SystemColor_INACTIVE_CAPTION_BORDER] = RgbColorToInt(ui_color(B_WINDOW_INACTIVE_TAB_COLOR));

    colors[java_awt_SystemColor_WINDOW]              = RgbColorToInt(ui_color(B_PANEL_BACKGROUND_COLOR));
    colors[java_awt_SystemColor_WINDOW_BORDER]       = RgbColorToInt(ui_color(B_WINDOW_BORDER_COLOR));
    colors[java_awt_SystemColor_WINDOW_TEXT]         = RgbColorToInt(ui_color(B_PANEL_TEXT_COLOR));
    colors[java_awt_SystemColor_MENU]                = RgbColorToInt(ui_color(B_MENU_BACKGROUND_COLOR));
    colors[java_awt_SystemColor_MENU_TEXT]           = RgbColorToInt(ui_color(B_MENU_ITEM_TEXT_COLOR));
    colors[java_awt_SystemColor_TEXT]                = RgbColorToInt(ui_color(B_DOCUMENT_BACKGROUND_COLOR));
    colors[java_awt_SystemColor_TEXT_TEXT]           = RgbColorToInt(ui_color(B_DOCUMENT_TEXT_COLOR));
    colors[java_awt_SystemColor_TEXT_HIGHLIGHT]      = 0xFF000000; // black
    colors[java_awt_SystemColor_TEXT_HIGHLIGHT_TEXT] = 0xFFFFFFFF; // white
    colors[java_awt_SystemColor_TEXT_INACTIVE_TEXT]  = 0xFFCCCCCC; // gray
    
    rgb_color controlColor                            = ui_color(B_CONTROL_BACKGROUND_COLOR);
    colors[java_awt_SystemColor_CONTROL]              = RgbColorToInt(controlColor);
    colors[java_awt_SystemColor_CONTROL_TEXT]         = RgbColorToInt(ui_color(B_CONTROL_TEXT_COLOR));
    colors[java_awt_SystemColor_CONTROL_HIGHLIGHT]    = RgbColorToInt(tint_color(controlColor, B_LIGHTEN_1_TINT));
    colors[java_awt_SystemColor_CONTROL_LT_HIGHLIGHT] = RgbColorToInt(tint_color(controlColor, B_LIGHTEN_2_TINT));
    colors[java_awt_SystemColor_CONTROL_SHADOW]       = RgbColorToInt(tint_color(controlColor, B_DARKEN_1_TINT));
    colors[java_awt_SystemColor_CONTROL_DK_SHADOW]    = RgbColorToInt(tint_color(controlColor, B_DARKEN_2_TINT));
    colors[java_awt_SystemColor_SCROLLBAR]            = RgbColorToInt(tint_color(controlColor, B_DARKEN_3_TINT));
    colors[java_awt_SystemColor_INFO]                 = RgbColorToInt(ui_color(B_TOOL_TIP_BACKGROUND_COLOR));
    colors[java_awt_SystemColor_INFO_TEXT]            = RgbColorToInt(ui_color(B_TOOL_TIP_TEXT_COLOR));

	env->ReleaseIntArrayElements(systemColors, colors, 0);
}

/*
 * Class:     sun_hawt_HaikuToolkit
 * Method:    nativeBeep
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuToolkit_nativeBeep(JNIEnv *env, jobject thiz)
{
	beep();
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void *reserved)
{
	jvm = vm;
    return JNI_VERSION_1_2;
}

// Here are a bunch of symbols that various Java classes need.
// We don't need to provide implementations for them.

JNIEXPORT void JNICALL
Java_sun_awt_SunToolkit_closeSplashScreen
  (JNIEnv *env, jclass clazz)
{
    typedef void (*SplashClose_t)();
    SplashClose_t splashClose;
    void* hSplashLib = dlopen(0, RTLD_LAZY);
    if (!hSplashLib) {
        return;
    }
    splashClose = (SplashClose_t)dlsym(hSplashLib,
        "SplashClose");
    if (splashClose) {
        splashClose();
    }
    dlclose(hSplashLib);
}

JNIEXPORT void JNICALL
Java_java_awt_event_InputEvent_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_event_KeyEvent_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_AWTEvent_nativeSetSource
  (JNIEnv *env, jobject thiz, jobject newSource)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Insets_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_AWTEvent_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Font_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Component_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_FileDialog_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Container_initIDs
  (JNIEnv *env, jclass clazz)
{

}


JNIEXPORT void JNICALL
Java_java_awt_Button_initIDs
  (JNIEnv *env, jclass clazz)
{

}

JNIEXPORT void JNICALL
Java_java_awt_Scrollbar_initIDs
  (JNIEnv *env, jclass clazz)
{

}


JNIEXPORT void JNICALL
Java_java_awt_Window_initIDs
  (JNIEnv *env, jclass clazz)
{

}

JNIEXPORT void JNICALL
Java_java_awt_Frame_initIDs
  (JNIEnv *env, jclass clazz)
{

}


JNIEXPORT void JNICALL
Java_java_awt_MenuComponent_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Cursor_initIDs
  (JNIEnv *env, jclass clazz)
{
}


JNIEXPORT void JNICALL
Java_java_awt_MenuItem_initIDs
  (JNIEnv *env, jclass clazz)
{
}


JNIEXPORT void JNICALL
Java_java_awt_Menu_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_TextArea_initIDs
  (JNIEnv *env, jclass clazz)
{
}


JNIEXPORT void JNICALL
Java_java_awt_Checkbox_initIDs
  (JNIEnv *env, jclass clazz)
{
}


JNIEXPORT void JNICALL
Java_java_awt_ScrollPane_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_TextField_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Dialog_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_KeyboardFocusManager_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_TrayIcon_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Cursor_finalizeImpl
  (JNIEnv *env, jclass clazz, jlong pData)
{
}


JNIEXPORT void JNICALL
Java_java_awt_Color_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_MenuBar_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Label_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_FontMetrics_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Toolkit_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_ScrollPaneAdjustable_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_CheckboxMenuItem_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Dimension_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Rectangle_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_event_MouseEvent_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Event_initIDs
  (JNIEnv *env, jclass clazz)
{
}

}
