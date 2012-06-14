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

#include "java_awt_event_MouseEvent.h"
#include "sun_hawt_HaikuTrayIcon.h"

#include <Alert.h>
#include <Application.h>
#include <Bitmap.h>
#include <Deskbar.h>
#include <File.h>
#include <FindDirectory.h>
#include <IconUtils.h>
#include <Notification.h>
#include <Path.h>
#include <Resources.h>
#include <View.h>

#include "Drawable.h"
#include "Utilities.h"

// Stole this from BAlert
static BBitmap*
GetIcon(alert_type alertType)
{
	// After a bit of a search, I found the icons in app_server. =P
	BBitmap* icon = NULL;
	BPath path;
	status_t status = find_directory(B_BEOS_SERVERS_DIRECTORY, &path);
	if (status < B_OK) {
		return NULL;
	}

	path.Append("app_server");
	BFile file;
	status = file.SetTo(path.Path(), B_READ_ONLY);
	if (status < B_OK) {
		return NULL;
	}

	BResources resources;
	status = resources.SetTo(&file);
	if (status < B_OK) {
		return NULL;
	}

	// Which icon are we trying to load?
	const char* iconName = "";	// Don't want any seg faults
	switch (alertType) {
		case B_INFO_ALERT:
			iconName = "info";
			break;
		case B_IDEA_ALERT:
			iconName = "idea";
			break;
		case B_WARNING_ALERT:
			iconName = "warn";
			break;
		case B_STOP_ALERT:
			iconName = "stop";
			break;

		default:
			// Alert type is either invalid or B_EMPTY_ALERT;
			// either way, we're not going to load an icon
			return NULL;
	}

	int32 iconSize = 32;
	// Allocate the icon bitmap
	icon = new(std::nothrow) BBitmap(BRect(0, 0, iconSize - 1, iconSize - 1),
		0, B_RGBA32);
	if (icon == NULL || icon->InitCheck() < B_OK) {
		delete icon;
		return NULL;
	}

	// Load the raw icon data
	size_t size = 0;
	const uint8* rawIcon;

#ifdef __HAIKU__
	// Try to load vector icon
	rawIcon = (const uint8*)resources.LoadResource(B_VECTOR_ICON_TYPE,
		iconName, &size);
	if (rawIcon != NULL
		&& BIconUtils::GetVectorIcon(rawIcon, size, icon) == B_OK) {
		return icon;
	}
#endif

	// Fall back to bitmap icon
	rawIcon = (const uint8*)resources.LoadResource(B_LARGE_ICON_TYPE,
		iconName, &size);
	if (rawIcon == NULL) {
		delete icon;
		return NULL;
	}

	// Handle color space conversion
#ifdef __HAIKU__
	if (icon->ColorSpace() != B_CMAP8) {
		BIconUtils::ConvertFromCMAP8(rawIcon, iconSize, iconSize,
			iconSize, icon);
	}
#else
	icon->SetBits(rawIcon, iconSize, 0, B_CMAP8);
#endif

	return icon;
}

static const int kMsgSetToolTip = 'sett';
static const int kMsgSetBitmap = 'setb';
static const int kMsgIconMessenger = 'msgr';

static const int kTrayIconSize = 16;

class TrayIconProxy : public BHandler {
public:
						TrayIconProxy(jobject peer, Drawable* drawable);
						~TrayIconProxy();

			void		SetId(int32 id) { fId = id; }
			int32		GetId() { return fId; }

			void		WaitForLink();
			void		SetToolTip(const char* tip);
			void		UpdateBitmap();

	virtual	void		MessageReceived(BMessage* message);

private:
			void		_HandleMouseEvent(BMessage* message);

private:
			jobject		fPeer;
			Drawable*	fDrawable;
			BMessenger	fIconMessenger;
			uint32		fId;
			sem_id		fLinkSem;

			uint32		fPreviousButtons;
			BPoint		fPreviousPoint;
};

TrayIconProxy::TrayIconProxy(jobject peer, Drawable* drawable)
	:
	BHandler("trayIconProxy"),
	fPeer(peer),
	fDrawable(drawable)
{
	fLinkSem = create_sem(0, "trayIconProxyLinkSem");
}

TrayIconProxy::~TrayIconProxy()
{
	GetEnv()->DeleteWeakGlobalRef(fPeer);
	delete_sem(fLinkSem);
}

void
TrayIconProxy::WaitForLink()
{
	while (acquire_sem(fLinkSem) == B_INTERRUPTED);
	release_sem(fLinkSem);
}

/*
 * NOTE
 * Call WaitForLink before calling any of the following functions
 */

void
TrayIconProxy::SetToolTip(const char* tip)
{
	BMessage message(kMsgSetToolTip);
	message.AddString("tip", tip);
	fIconMessenger.SendMessage(&message);
}

void
TrayIconProxy::UpdateBitmap()
{
	if (fDrawable->Lock()) {
		if (fDrawable->IsValid()) {
			BMessage message(kMsgSetBitmap);

			BMessage bitmapArchive;
			BBitmap* bitmap = fDrawable->GetBitmap();
			bitmap->Archive(&bitmapArchive, true);
			message.AddMessage("bitmapArchive", &bitmapArchive);

			fIconMessenger.SendMessage(&message);
		}
		fDrawable->Unlock();
	}
}

void
TrayIconProxy::MessageReceived(BMessage* message)
{
	switch (message->what) {
		case kMsgIconMessenger: {
			// There are a lot of error cases here where there's
			// no appropriate way to propogate the error back to Java.
			// So I just assume that everything works fine...
			
			message->FindMessenger("iconMessenger", &fIconMessenger);
			release_sem(fLinkSem);
			break;
		}
		case B_MOUSE_DOWN:
		case B_MOUSE_MOVED:
		case B_MOUSE_UP:
			_HandleMouseEvent(message);
			break;
		default:
			break;
	}
	BHandler::MessageReceived(message);
}

DECLARE_JAVA_CLASS(trayIconClazz, "sun/hawt/HaikuTrayIcon")

void
TrayIconProxy::_HandleMouseEvent(BMessage* message)
{
	BPoint point;
	message->FindPoint("screenPoint", &point);

	// Get out early if this message is useless
	int32 buttons = 0;
	message->FindInt32("buttons", &buttons);
	int32 buttonChange = buttons ^ fPreviousButtons;
	if (point == fPreviousPoint && buttonChange == 0)
		return;

	fPreviousPoint = point;
	fPreviousButtons = buttons;

	int64 when = 0;
	message->FindInt64("when", &when);
	int32 clicks = 0;
	message->FindInt32("clicks", &clicks);

	int32 modifiers = 0;
	if (message->FindInt32("modifiers", &modifiers) != B_OK)
		modifiers = ::modifiers();
	jint mods = ConvertInputModifiersToJava(modifiers)
		| ConvertMouseMaskToJava(buttons);

	jint id = 0;
	switch (message->what) {
		case B_MOUSE_DOWN:
			id = java_awt_event_MouseEvent_MOUSE_PRESSED;
			break;
		case B_MOUSE_UP:
			id = java_awt_event_MouseEvent_MOUSE_RELEASED;
			break;
		case B_MOUSE_MOVED:
			id = java_awt_event_MouseEvent_MOUSE_MOVED;
			break;
		default:
			break;
	}

	jint button = java_awt_event_MouseEvent_NOBUTTON;
	if (id == java_awt_event_MouseEvent_MOUSE_PRESSED
			|| id == java_awt_event_MouseEvent_MOUSE_RELEASED)
		button = ConvertMouseButtonToJava(buttonChange);

	JNIEnv* env = GetEnv();

	DECLARE_VOID_JAVA_METHOD(handleMouse, trayIconClazz, "handleMouseEvent",
		"(IJIIIII)V");
	env->CallVoidMethod(fPeer, handleMouse, id, (jlong)(when / 1000), mods,
		(jint)point.x, (jint)point.y, (jint)clicks, button);
}

class TrayIcon : public BView {
public:
						TrayIcon(BMessenger proxyMessenger);
						TrayIcon(BMessage* archive);
						~TrayIcon();

	virtual	TrayIcon*	Instantiate(BMessage* archive);
	virtual	status_t	Archive(BMessage* message, bool deep) const;

	virtual	void		AttachedToWindow();
	virtual	void		DetachedFromWindow();
	virtual	void		Draw(BRect updateRect);
	virtual	void		MessageReceived(BMessage* message);
	virtual	void		MouseDown(BPoint point);
	virtual	void		MouseMoved(BPoint point, uint32 transit,
							const BMessage* dragMessage);
	virtual	void		MouseUp(BPoint point);

private:
			void		_HandleMouseEvent(BPoint point);

private:
			BMessenger	fProxyMessenger;
			BBitmap*	fBitmap;
};

TrayIcon::TrayIcon(BMessenger proxyMessenger)
	:
	BView(BRect(0, 0, kTrayIconSize - 1, kTrayIconSize - 1), "awtTrayIcon",
		B_FOLLOW_ALL, B_WILL_DRAW),
	fProxyMessenger(proxyMessenger),
	fBitmap(NULL)
{
}

TrayIcon::TrayIcon(BMessage* archive)
	:
	BView(archive)
{
	archive->FindMessenger("proxyMessenger", &fProxyMessenger);
}

TrayIcon::~TrayIcon()
{
	if (fBitmap != NULL) {
		delete fBitmap;
		fBitmap = NULL;
	}
}

TrayIcon*
TrayIcon::Instantiate(BMessage* archive)
{
	if (!validate_instantiation(archive, "TrayIcon"))
		return NULL;

	return new(std::nothrow) TrayIcon(archive);
}

status_t
TrayIcon::Archive(BMessage* archive, bool deep) const
{
	status_t status = BView::Archive(archive, deep);
	if (status == B_OK)
		status = archive->AddString("class", "TrayIcon");
	if (status == B_OK)
		status = archive->AddMessenger("proxyMessenger", fProxyMessenger);

	return status;
}

void
TrayIcon::AttachedToWindow()
{
	BMessage message(kMsgIconMessenger);
	
	BMessenger iconMessenger(this);
	message.AddMessenger("iconMessenger", iconMessenger);
	fProxyMessenger.SendMessage(&message);
}

void
TrayIcon::DetachedFromWindow()
{
	// ...
}

void
TrayIcon::Draw(BRect updateRect)
{
	if (fBitmap != NULL)
		DrawBitmapAsync(fBitmap, updateRect, updateRect);
}

void
TrayIcon::MessageReceived(BMessage* message)
{
	switch (message->what) {
		case kMsgSetBitmap: {
			BMessage archive;
			if (message->FindMessage("bitmapArchive", &archive) == B_OK) {
				BBitmap* bitmap = new BBitmap(&archive);
				if (bitmap->IsValid()) {
					if (fBitmap != NULL) {
						delete fBitmap;
					}
					fBitmap = bitmap;
				}
			}
			break;
		}
		case kMsgSetToolTip: {
			const char* tip;
			message->FindString("tip", &tip);
			SetToolTip(tip);
			break;
		}
		default:
			break;
	}
	TrayIcon::MessageReceived(message);
}

void
TrayIcon::MouseDown(BPoint point)
{
	_HandleMouseEvent(point);
}

void
TrayIcon::MouseMoved(BPoint point, uint32 transit, const BMessage* dragMessage)
{
	_HandleMouseEvent(point);
}

void
TrayIcon::MouseUp(BPoint point)
{
	_HandleMouseEvent(point);
}

void
TrayIcon::_HandleMouseEvent(BPoint point)
{
	ConvertToScreen(&point);

	BMessage* message = Window()->CurrentMessage();
	message->AddPoint("screenPoint", point);
	fProxyMessenger.SendMessage(message);
}

extern "C" {

/*
 * Class:     sun_hawt_HaikuTrayIcon
 * Method:    nativeCreate
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_hawt_HaikuTrayIcon_nativeCreate(JNIEnv *env, jobject thiz,
	jlong nativeDrawable)
{
	// released in TrayIconProxy destructor
	jobject peer = env->NewWeakGlobalRef(thiz);
	if (peer == NULL) {
		return 0;
	}

	Drawable* drawable = (Drawable*)jlong_to_ptr(nativeDrawable);
	TrayIconProxy* proxy = new TrayIconProxy(peer, drawable);
	be_app->AddHandler(proxy);

	BMessenger proxyMessenger(proxy);
	TrayIcon icon(proxyMessenger);

	int32 id;
	BDeskbar deskbar;
	deskbar.AddItem(&icon, &id);
	proxy->SetId(id);

	// Wait for the proxy to establish the link with the icon
	// in the deskbar before returning to Java
	proxy->WaitForLink();

	return ptr_to_jlong(proxy);
}

/*
 * Class:     sun_hawt_HaikuTrayIcon
 * Method:    nativeSetToolTip
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuTrayIcon_nativeSetToolTip(JNIEnv *env, jobject thiz,
	jlong nativeTrayIconProxy, jstring toolTip)
{
	const char* tip = env->GetStringUTFChars(toolTip, NULL);
	if (tip == NULL) {
		return;
	}

	TrayIconProxy* proxy = (TrayIconProxy*)jlong_to_ptr(nativeTrayIconProxy);
	proxy->SetToolTip(tip);
	env->ReleaseStringUTFChars(toolTip, tip);
}

/*
 * Class:     sun_hawt_HaikuTrayIcon
 * Method:    nativeDisplayMessage
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuTrayIcon_nativeDisplayMessage(JNIEnv *env, jobject thiz,
	jstring caption, jstring text, jint messageType)
{
	const char* title = env->GetStringUTFChars(caption, NULL);
	if (title == NULL) {
		return;
	}

	const char* content = env->GetStringUTFChars(text, NULL);
	if (content == NULL) {
		env->ReleaseStringUTFChars(caption, title);
		return;
	}

	BNotification notification(B_INFORMATION_NOTIFICATION);
	notification.SetGroup("javaAwtApp");
	notification.SetTitle(title);
	notification.SetContent(content);

	BBitmap* icon = NULL;
	if (messageType == sun_hawt_HaikuTrayIcon_INFO_ICON) {
		icon = GetIcon(B_INFO_ALERT);
	} else if (messageType == sun_hawt_HaikuTrayIcon_WARNING_ICON) {
		icon = GetIcon(B_WARNING_ALERT);
	} else if (messageType == sun_hawt_HaikuTrayIcon_ERROR_ICON) {
		icon = GetIcon(B_STOP_ALERT);
	}

	if (icon != NULL) {
		notification.SetIcon(icon);
	}

	notification.Send();

	env->ReleaseStringUTFChars(caption, title);
	env->ReleaseStringUTFChars(text, content);
}

/*
 * Class:     sun_hawt_HaikuTrayIcon
 * Method:    nativeUpdate
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuTrayIcon_nativeUpdate(JNIEnv *env, jobject thiz,
	jlong nativeTrayIconProxy)
{
	TrayIconProxy* proxy = (TrayIconProxy*)jlong_to_ptr(nativeTrayIconProxy);
	proxy->UpdateBitmap();
}

/*
 * Class:     sun_hawt_HaikuTrayIcon
 * Method:    nativeDispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_hawt_HaikuTrayIcon_nativeDispose(JNIEnv *env, jobject thiz,
	jlong nativeTrayIconProxy)
{
	TrayIconProxy* proxy = (TrayIconProxy*)jlong_to_ptr(nativeTrayIconProxy);

	// Not much we can do if this fails
	BDeskbar().RemoveItem(proxy->GetId());

	be_app->RemoveHandler(proxy);
	delete proxy;
}

}
