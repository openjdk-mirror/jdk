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

#include <jlong.h>
#include <jni.h>

#include <Application.h>
#include <Menu.h>
#include <MenuBar.h>
#include <MenuItem.h>
#include <PopUpMenu.h>

extern "C" {

extern sem_id appSem;

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeCreateMenu
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuMenu_nativeCreateMenu(JNIEnv *env, jobject thiz)
{
	BMenu* menu = new BMenu("awtMenu");
	BMenuItem* item = new BMenuItem(menu);
	return ptr_to_jlong(item);
}

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeSetLabel
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenu_nativeSetLabel(JNIEnv *env, jobject thiz,
	jlong menuPtr, jstring label)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(menuPtr);
	BMenu* menu = menuItem->Submenu();

	const char* name = env->GetStringUTFChars(label, NULL);
	if (name == NULL)
		return;

	menu->SetName(name);
	env->ReleaseStringUTFChars(label, name);
}

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeSetEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenu_nativeSetEnabled(JNIEnv *env, jobject thiz,
	jlong menuPtr, jboolean enabled)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(menuPtr);
	BMenu* menu = menuItem->Submenu();
	menu->SetEnabled(enabled);
}

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeAddSeparator
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenu_nativeAddSeparator(JNIEnv *env, jobject thiz,
	jlong menuPtr)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(menuPtr);
	BMenu* menu = menuItem->Submenu();
	menu->AddSeparatorItem();
}

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeAddItem
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenu_nativeAddItem(JNIEnv *env, jobject thiz,
	jlong menuPtr, jlong itemPtr)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(menuPtr);
	BMenu* menu = menuItem->Submenu();
	BMenuItem* item = (BMenuItem*)jlong_to_ptr(itemPtr);
	menu->AddItem(item);
}

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeDeleteItem
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenu_nativeDeleteItem(JNIEnv *env, jobject thiz,
	jlong menuPtr, jint index)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(menuPtr);
	BMenu* menu = menuItem->Submenu();
	menu->RemoveItem(index);
}

/*
 * Class:     sun_hawt_HaikuMenuBar
 * Method:    nativeCreateMenuBar
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuMenuBar_nativeCreateMenuBar(JNIEnv *env, jobject thiz)
{
	BMenuBar* bar = new BMenuBar(BRect(0, 0, 0, 0), "awtMenuBar");
	return ptr_to_jlong(bar);
}

/*
 * Class:     sun_hawt_HaikuMenuBar
 * Method:    nativeAddMenu
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenuBar_nativeAddMenu(JNIEnv *env, jobject thiz,
	jlong menuBarPtr, jlong menuPtr)
{
	BMenuBar* menuBar = (BMenuBar*)jlong_to_ptr(menuBarPtr);
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(menuPtr);
	menuBar->AddItem(menuItem);
}

/*
 * Class:     sun_hawt_HaikuMenuBar
 * Method:    nativeDelMenu
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenuBar_nativeDelMenu(JNIEnv *env, jobject thiz,
	jlong menuBarPtr, jint index)
{
	BMenuBar* menuBar = (BMenuBar*)jlong_to_ptr(menuBarPtr);
	menuBar->RemoveItem(index);
}

/*
 * Class:     sun_hawt_HaikuMenuItem
 * Method:    nativeCreateMenuItem
 * Signature: (Z)J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuMenuItem_nativeCreateMenuItem(JNIEnv *env, jobject thiz,
	jboolean separator)
{
	BMessage* msg = new BMessage('menu');
	jobject peer = env->NewWeakGlobalRef(thiz);
	msg->AddPointer("peer", &peer);
	
	BMenuItem* item = new BMenuItem("awtMenuItem", msg);
	
	// Wait for be_app to get created
	acquire_sem(appSem);
	release_sem(appSem);
	item->SetTarget(be_app);
	return ptr_to_jlong(item);
}

/*
 * Class:     sun_hawt_HaikuMenuItem
 * Method:    nativeSetLabel
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenuItem_nativeSetLabel(JNIEnv *env, jobject thiz,
	jlong itemPtr, jstring label)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(itemPtr);

	const char* name = env->GetStringUTFChars(label, NULL);
	if (name == NULL)
		return;

	menuItem->SetLabel(name);
	env->ReleaseStringUTFChars(label, name);
}

/*
 * Class:     sun_hawt_HaikuMenuItem
 * Method:    nativeSetEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenuItem_nativeSetEnabled(JNIEnv *env, jobject thiz,
	jlong itemPtr, jboolean enabled)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(itemPtr);
	menuItem->SetEnabled(enabled);
}

/*
 * Class:     sun_hawt_HaikuMenuItem
 * Method:    nativeCreateCheckboxMenuItem
 * Signature: (Z)J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuCheckboxMenuItem_nativeCreateCheckboxMenuItem(JNIEnv *env,
	jobject thiz, jboolean separator)
{
	BMessage* msg = new BMessage('menu');
	jobject peer = env->NewWeakGlobalRef(thiz);
	msg->AddPointer("peer", &peer);

	// We add this bool so the handler knows to do the extra stuff
	msg->AddBool("checkbox", true);

	BMenuItem* item = new BMenuItem("awtMenuItem", msg);
	
	// Wait for be_app to get created
	acquire_sem(appSem);
	release_sem(appSem);
	item->SetTarget(be_app);
	return ptr_to_jlong(item);
}

/*
 * Class:     sun_hawt_HaikuCheckboxMenuItem
 * Method:    nativeSetState
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuCheckboxMenuItem_nativeSetState(JNIEnv *env, jobject thiz,
	jlong itemPtr, jboolean state)
{
	BMenuItem* menuItem = (BMenuItem*)jlong_to_ptr(itemPtr);
	menuItem->SetMarked(state);
}

/*
 * Class:     sun_hawt_HaikuPopupMenu
 * Method:    nativeCreatePopupMenu
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuPopupMenu_nativeCreatePopupMenu(JNIEnv *env, jobject thiz)
{
	BPopUpMenu* popupMenu = new BPopUpMenu("awtPopupMenu", false, false);
	// not so sure why I thought it was a good idea to put everything
	// in BMenuItems
	BMenuItem* popupMenuItem = new BMenuItem(popupMenu);
	return ptr_to_jlong(popupMenuItem);
}

/*
 * Class:     sun_hawt_HaikuPopupMenu
 * Method:    nativeShowPopupMenu
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuPopupMenu_nativeShowPopupMenu(JNIEnv *env, jobject thiz,
	jlong popupMenuPtr, jint x, jint y)
{
	BMenuItem* popupMenuItem = (BMenuItem*)jlong_to_ptr(popupMenuPtr);
	BPopUpMenu* popupMenu = (BPopUpMenu*)popupMenuItem->Submenu();
	
	popupMenu->Go(BPoint(x, y), true, false, true);
}

}
