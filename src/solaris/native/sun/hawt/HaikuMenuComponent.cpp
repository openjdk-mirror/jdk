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

#include "AwtApplication.h"
#include "Utilities.h"

extern "C" {

/*
 * Class:     sun_hawt_HaikuMenuComponent
 * Method:    nativeDispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenuComponent_nativeDispose(JNIEnv *env, jobject thiz,
	jlong itemPtr)
{
	BMenuItem* item = (BMenuItem*)jlong_to_ptr(itemPtr);
	BMenu* parent = item->Menu();
	if (parent != NULL) {
		parent->RemoveItem(item);
	}

	delete item;
}

/*
 * Class:     sun_hawt_HaikuMenu
 * Method:    nativeCreateMenu
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuMenu_nativeCreateMenu(JNIEnv *env, jobject thiz, jlong menuItemPtr)
{
	BMenu* menu = new BMenu("awtMenu");
	BMenuItem* item = new BMenuItem(menu);
	
	BMenuItem* parentItem = (BMenuItem*)jlong_to_ptr(menuItemPtr);
	BMenu* parentMenu = parentItem->Submenu();
	parentMenu->AddItem(item);

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

	const char* nativeLabel = env->GetStringUTFChars(label, NULL);
	if (nativeLabel == NULL)
		return;

	menuItem->SetLabel(nativeLabel);
	env->ReleaseStringUTFChars(label, nativeLabel);
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
	BMenuItem* item = new BMenuItem(bar);
	return ptr_to_jlong(item);
}

/*
 * Class:     sun_hawt_HaikuMenuBar
 * Method:    nativeAddMenu
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuMenuBar_nativeAddMenu(JNIEnv *env, jobject thiz,
	jlong menuBarItemPtr, jlong menuPtr)
{
	BMenuItem* menuBarItem = (BMenuItem*)jlong_to_ptr(menuBarItemPtr);
	BMenuBar* menuBar = (BMenuBar*)menuBarItem->Submenu();
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
	jlong menuBarItemPtr, jint index)
{
	BMenuItem* menuBarItem = (BMenuItem*)jlong_to_ptr(menuBarItemPtr);
	BMenuBar* menuBar = (BMenuBar*)menuBarItem->Submenu();
	menuBar->RemoveItem(index);
}

/*
 * Class:     sun_hawt_HaikuMenuItem
 * Method:    nativeCreateMenuItem
 * Signature: (Z)J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuMenuItem_nativeCreateMenuItem(JNIEnv *env, jobject thiz,
	jlong menuItemPtr, jboolean separator)
{
	BMessage* msg = new BMessage(kMenuMessage);
	jobject peer = env->NewWeakGlobalRef(thiz);
	msg->AddPointer("peer", (void*)peer);
	
	BMenuItem* item;
	if (separator) {
		item = new BSeparatorItem();
	} else {
		item = new BMenuItem("awtMenuItem", msg);
	}

	BMenuItem* parentItem = (BMenuItem*)jlong_to_ptr(menuItemPtr);
	BMenu* parentMenu = parentItem->Submenu();
	parentMenu->AddItem(item);

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

	const char* nativeLabel = env->GetStringUTFChars(label, NULL);
	if (nativeLabel == NULL)
		return;

	menuItem->SetLabel(nativeLabel);
	env->ReleaseStringUTFChars(label, nativeLabel);
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
	jobject thiz, jlong menuItemPtr)
{
	BMessage* msg = new BMessage(kMenuMessage);
	jobject peer = env->NewWeakGlobalRef(thiz);
	msg->AddPointer("peer", (void*)peer);

	// We add this bool so the handler knows to do the extra stuff
	msg->AddBool("checkbox", true);

	BMenuItem* item = new BMenuItem("awtMenuItem", msg);

	BMenuItem* parentItem = (BMenuItem*)jlong_to_ptr(menuItemPtr);
	BMenu* parentMenu = parentItem->Submenu();
	parentMenu->AddItem(item);

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
