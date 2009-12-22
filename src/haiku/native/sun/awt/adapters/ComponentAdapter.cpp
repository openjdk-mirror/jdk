#include "ComponentAdapter.h"
#include "ContainerAdapter.h"
#include "GlobalCursorAdapter.h"
#include "MenuItemAdapter.h"
#include "Window.h"
#include "EventEnvironment.h"
#include "KeyboardFocusManager.h"
#include "KeyConversions.h"
#include "Color.h"
#include <Bitmap.h>
#include <ByteOrder.h>
#include <Message.h>
#include <String.h>
#include <View.h>
#include <typeinfo>


/* static */ BRect
ComponentAdapter::GetFrame(JNIEnv * jenv, jobject jpeer)
{
	jobject jtarget = jenv->GetObjectField(jpeer, javaObject_ID);
	float x = (float)jenv->GetIntField(jtarget, x_ID);
	float y = (float)jenv->GetIntField(jtarget, y_ID);
	float width = (float)jenv->GetIntField(jtarget, width_ID);
	float height = (float)jenv->GetIntField(jtarget, height_ID);
	jenv->DeleteLocalRef(jtarget);
	return BRect(x, y, x + width, y + height);
}


ComponentAdapter::ComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BView * view)
	: ObjectAdapter(),
	  jparent(NULL), surface(NULL), view(NULL), visible(true),
	  oldModifiers(0), oldButtons(0), oldClicks(0), oldX(0), oldY(0)
{
	this->view = view;
	CreateSurface(view->Bounds(), false);
	
	if (jparent != NULL) {
		this->jparent = jenv->NewGlobalRef(jparent);
		if (view->Window() == NULL) {
			// Root views already belong to a window.
			// All other views should be immediately attached to their parent.
			ObjectAdapter * adapter = (ObjectAdapter*)jenv->GetLongField(jparent, nativeObject_ID);
			if (adapter != NULL) {
				dynamic_cast<ContainerAdapter*>(adapter)->AttachChild(view);
			}
		}
	}
	
	LinkObjects(jenv, jpeer);
}


ComponentAdapter::~ComponentAdapter()
{
	DASSERT(view == NULL);
	// If we have a surface, we're the owner of it.
	if (surface != NULL) {
		delete surface;
		surface = NULL;
	}
	UnlinkObjects();
	if (jparent != NULL) {
		JNIEnv * jenv = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
		jenv->DeleteGlobalRef(jparent);
		jparent = NULL;
	}
}


// #pragma mark -
//
// JNI entry points
//

void
ComponentAdapter::hide()
{
	if (view->LockLooper()) {
		if (visible) {
			view->Hide();
			visible = false;
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::show()
{
	if (view->LockLooper()) {
		if (!visible) {
			view->Show();
			visible = true;
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::updateWindow()
{
	view->Sync(); // Wait for any paint events.
}


void
ComponentAdapter::dispose()
{
	if (view != NULL) {
		if (view->LockLooper()) {
			BWindow * window = view->Window();
			view->RemoveSelf(); // after RemoveSelf the view has no looper
			window->UnlockLooper();
			delete view;
		} else if (view->Window() == NULL) {
			// No window means no need to lock a looper.
			// Component views should not be attached to parents if they are not attached to windows.
			DASSERT(view->Parent() == NULL);
			delete view;
		} else {
			LOCK_LOOPER_FAILED();
		}
		view = NULL;
	}
	delete this;
}


BPoint
ComponentAdapter::getLocationOnScreen()
{
	BPoint location;
	if (view->LockLooper()) {
		location = view->ConvertToScreen(BPoint(0, 0));
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	return location;
}


void
ComponentAdapter::nativeHandleEvent(jobject awtEvent)
{
	// TODO? (noisy)
}


void
ComponentAdapter::reshape(int x, int y, int width, int height)
{
	if (view->LockLooper()) {
		DASSERT(view->Parent() != NULL); // should not be called on the root view
		view->MoveTo(x, y);
		view->ResizeTo(width, height);
		RecreateSurface(true);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::setForeground(rgb_color color)
{
	if (view->LockLooper()) {
		view->SetHighColor(color);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::setBackground(rgb_color color)
{
	if (view->LockLooper()) {
		view->SetViewColor(color);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::setFont(BFont font)
{
	if (view->LockLooper()) {
		view->SetFont(&font);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::addDropTarget(jobject jdropTarget)
{
	PRINT_PRETTY_FUNCTION();
}


void
ComponentAdapter::removeDropTarget(jobject jdropTarget)
{
	PRINT_PRETTY_FUNCTION();
}


// #pragma mark -
//
// utility functions
//

/* virtual */ EventEnvironment *
ComponentAdapter::Environment()
{
	Window * window = dynamic_cast<Window*>(view->Window());
	DASSERT(window != NULL);
	EventEnvironment * environment = window->Environment();
	DASSERT(environment != NULL);
	DASSERT(environment->env == GetEnv());
	return environment;
}


BMessage *
ComponentAdapter::CurrentMessage()
{
	BWindow * window = view->Window();
	DASSERT(window != NULL);
	BMessage * message = window->CurrentMessage();
	DASSERT(message != NULL);
	return message;
}


void
ComponentAdapter::FireMouseEvent()
{
	BMessage * message = CurrentMessage();
	EventEnvironment * environment = Environment();
	JNIEnv * env = environment->env;
	int64 when;
	if (message->FindInt64("when", &when) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
	}
	int32 buttons = 0;
	message->FindInt32("buttons", &buttons);
	int32 modifiers = 0;
	if (message->FindInt32("modifiers", &modifiers) != B_OK) {
		modifiers = ::modifiers();
	}
	int32 clicks = 0;
	message->FindInt32("clicks", &clicks);
	BPoint view_where;
	message->FindPoint("be:view_where", &view_where);
	BPoint java_where = view_where; // + GetInsets().LeftTop();
	int32 transit;
	message->FindInt32("be:transit", &transit);
	bool popupTrigger = false;
	// haiku mouse up/down note:
	// after a button is pressed, we get no messages for any other button
	// presses or releases.  the release message does not contain the
	// identifier for the button released either.
	int button = (buttons & ~oldButtons) | (oldButtons & ~buttons);
	switch (message->what) {
	case B_MOUSE_DOWN:
		popupTrigger = true;
		break;
	case B_MOUSE_UP:
		clicks = oldClicks;
		break;
	case B_MOUSE_MOVED:
		break;
	default: {
		fprintf(stdout, "%s: unexpected message: ", __PRETTY_FUNCTION__);
		fprint_message(stdout, message);
		fprintf(stdout, "\n");
		return;
	}
	}
	
	// Prior to sending the java events (that get processed async)
	// update the state of the GlobalCursorAdapter. Doing ConvertToScreen
	// here frees us from having to deal with extra BWindow locking.
	BPoint position = GetView()->ConvertToScreen(view_where);
	ComponentAdapter * adapter = (transit == B_EXITED_VIEW ? NULL : this);
	GlobalCursorAdapter::GetInstance()->SetMouseOver(position, adapter);
	
	// convert to java
	jint jid = (message->what == B_MOUSE_DOWN ? java_awt_event_MouseEvent_MOUSE_PRESSED :
	            message->what == B_MOUSE_UP ? java_awt_event_MouseEvent_MOUSE_RELEASED :
	            transit == B_ENTERED_VIEW ? java_awt_event_MouseEvent_MOUSE_ENTERED :
	            transit == B_EXITED_VIEW ? java_awt_event_MouseEvent_MOUSE_EXITED :
	            buttons == 0 ? java_awt_event_MouseEvent_MOUSE_MOVED :
	                           java_awt_event_MouseEvent_MOUSE_DRAGGED);
	jlong jwhen = when;
	jint jmodifiers = ConvertInputModifiersToJava(modifiers);
	if (buttons & B_PRIMARY_MOUSE_BUTTON) {
		jmodifiers |= java_awt_event_MouseEvent_BUTTON1_DOWN_MASK;
	}
	if (buttons & B_SECONDARY_MOUSE_BUTTON) {
		jmodifiers |= java_awt_event_MouseEvent_BUTTON2_DOWN_MASK;
	}
	if (buttons & B_TERTIARY_MOUSE_BUTTON) {
		jmodifiers |= java_awt_event_MouseEvent_BUTTON3_DOWN_MASK;
	}
	jint jx = (jint)java_where.x;
	jint jy = (jint)java_where.y;
	if ((message->what == B_MOUSE_MOVED) && (oldX == jx) && (oldY == jy) &&
		(oldButtons == buttons) && (oldClicks == clicks) && (oldModifiers == modifiers)) {
		// silently eat spuriously repeated mouse moves (occurs every pulse on some views)
		return;
	}
	jint jclickCount = clicks;
	jboolean jpopupTrigger = popupTrigger && (buttons & B_SECONDARY_MOUSE_BUTTON);
	jint jbutton = (button == B_PRIMARY_MOUSE_BUTTON ? java_awt_event_MouseEvent_BUTTON1 :
	                button == B_SECONDARY_MOUSE_BUTTON ? java_awt_event_MouseEvent_BUTTON2 :
	                button == B_TERTIARY_MOUSE_BUTTON ? java_awt_event_MouseEvent_BUTTON3 : 
	                                                    java_awt_event_MouseEvent_NOBUTTON);
	jobject target = GetTarget(env);
	jobject event = environment->NewMouseEvent(target, jid, jwhen, jmodifiers, jx, jy,
	                                           jclickCount, jpopupTrigger, jbutton);
	
	// If the view isn't focused and this is a mouse-down, we signal the FocusManager.
	if ((!view->IsFocus()) && jid == java_awt_event_MouseEvent_MOUSE_PRESSED) {
		KeyboardFocusManager::GetInstance()->heavyweightButtonDown(env, target, jwhen);
	}
	
	// send out the appropriate event
	SendEvent(event);
	env->DeleteLocalRef(event);
	event = NULL;
	if ((message->what == B_MOUSE_UP) && (jclickCount != 0)) {
		event = environment->NewMouseEvent(target, java_awt_event_MouseEvent_MOUSE_CLICKED,
	                                       jwhen, jmodifiers, jx, jy, jclickCount,
	                                       jpopupTrigger, jbutton);
		SendEvent(event);
		env->DeleteLocalRef(event);
		event = NULL;
	}
	env->DeleteLocalRef(target);
	target = NULL;
	// store state
	oldModifiers = modifiers;
	oldButtons = buttons;
	oldClicks = clicks;
	oldX = jx;
	oldY = jy;
}


void
ComponentAdapter::FireKeyboardEvent()
{
	BMessage * message = CurrentMessage();
	EventEnvironment * environment = Environment();
	JNIEnv * env = environment->env;
	BWindow * window = view->Window();
	BView * view = (window ? window->CurrentFocus() : NULL);
	const char * name = (view ? (view->Name() ? view->Name() : "<no name>") : "<no view>");

	// Information we need from the BMessage
	int64 when = 0;      // Event time, in microseconds since 01/01/1970
	int32 modifiers = 0; // The modifier keys that were in effect at the time of the event.
	                     // Includes bits for caps/num/scroll lock, shift, cntrl, alt, etc.
	int32 key = 0;       // The code for the physical key that was pressed.
	BString bytes;       // String resulting from a key being typed.
	
	switch (message->what) {
	case B_KEY_DOWN:
	case B_KEY_UP:
	case B_UNMAPPED_KEY_DOWN:
	case B_UNMAPPED_KEY_UP:
		break;
	default: {
		fprintf(stdout, "%s: unexpected message: ", __PRETTY_FUNCTION__);
		fprint_message(stdout, message);
		fprintf(stdout, "\n");
		return;
	}
	}
	message->FindInt64("when", &when);
	
	// Java KeyEvent fields
	jobject jtarget = GetTarget(env);
	jint jid;
	jlong jwhen = when;
	jint jmodifiers = 0;
	jint jkeyCode = java_awt_event_KeyEvent_VK_UNDEFINED;
	jint jkeyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;
	jchar jkeyChar = java_awt_event_KeyEvent_CHAR_UNDEFINED;
		
	// Key Pressed / Released
	// These are triggered by an input device that generates keycodes.
	if ((message->FindInt32("modifiers", &modifiers) == B_OK) &&
	    (message->FindInt32("key", &key) == B_OK))
	{
		if (message->what == B_KEY_DOWN || message->what == B_UNMAPPED_KEY_DOWN) {
			jid = java_awt_event_KeyEvent_KEY_PRESSED;
		} else {
			jid = java_awt_event_KeyEvent_KEY_RELEASED;
		}
		jmodifiers = ConvertInputModifiersToJava(modifiers);
		ConvertKeyCodeToJava(key, modifiers, &jkeyCode, &jkeyLocation);
		jobject jevent = environment->NewKeyEvent(jtarget, jid, jwhen, jmodifiers,
		                                          jkeyCode, jkeyChar, jkeyLocation);
		SendEvent(jevent);
		env->DeleteLocalRef(jevent);
		jevent = NULL;
	}
	
	// Key Typed (Consists of potentially more than one Pressed/Release cycles)
	// messages with a bytes field are the result of character input events. This includes
	// input methods where the keyboard may not actually be used.
	if (message->FindString("bytes", &bytes) == B_OK) {
		// If we hava a key field that's non-zero, respond on KEY_UP.
		// If we don't have a key field (key is zero), we need to fire on KEY_DOWN.
		if ((key != 0 && (message->what == B_KEY_UP || message->what == B_UNMAPPED_KEY_UP)) ||
		    (key == 0 && (message->what == B_KEY_DOWN || message->what == B_UNMAPPED_KEY_DOWN)))
		{
			jid = java_awt_event_KeyEvent_KEY_TYPED;
			jmodifiers = ConvertInputModifiersToJava(modifiers);
			jkeyCode = java_awt_event_KeyEvent_VK_UNDEFINED;
			jkeyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;
			jkeyChar = bytes.ByteAt(0);
			jobject jevent = environment->NewKeyEvent(jtarget, jid, jwhen, jmodifiers,
			                                          jkeyCode, jkeyChar, jkeyLocation);
			SendEvent(jevent);
			env->DeleteLocalRef(jevent);
			jevent = NULL;
		}
	}
	
	env->DeleteLocalRef(jtarget);
	jtarget = NULL;
}


void
ComponentAdapter::ModifiersChanged()
{
	BMessage * message = CurrentMessage();
	int32 modifiers, old_modifiers;
	uchar states_ptr[16];
    ssize_t size_of_states_ptr = sizeof(states_ptr);
	if ((message->FindData("states", B_UINT8_TYPE, (const void**)&states_ptr, &size_of_states_ptr) != B_OK) ||
        (size_of_states_ptr != sizeof(states_ptr)) ||
		(message->FindInt32("modifiers", &modifiers) != B_OK) ||
		(message->FindInt32("be:old_modifiers", &old_modifiers) != B_OK)) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	EventEnvironment * environment = Environment();
	JNIEnv * env = environment->env;
	TODO();
}


void
ComponentAdapter::MouseWheelChanged()
{
	PRINT_PRETTY_FUNCTION();
	// parse the message
	BMessage * message = CurrentMessage();
	int64 when = 0;
	float delta_x, delta_y;
	if ((message->FindFloat("be:wheel_delta_x", &delta_x) != B_OK) ||
		(message->FindFloat("be:wheel_delta_y", &delta_y) != B_OK)) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	// build the event
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jevent = NULL;
	jint jid = java_awt_event_MouseEvent_MOUSE_WHEEL;
	jint jtype = java_awt_event_MouseWheelEvent_WHEEL_UNIT_SCROLL;
// questionable code...
	if (delta_y == 0) {
		// can't do horizontal scrolls?
		return;
	}
	jint jmodifiers = ConvertInputModifiersToJava(modifiers());
	jint jx = 0;
	jint jy = 0;
	jint jscrollAmount = 1;
	jint jwheelRotation = (int)(10*delta_y);
//
	jobject jtarget = GetTarget(jenv);
	jevent = environment->NewMouseWheelEvent(jtarget, jid, when, jmodifiers, jx, jy, 0, 
	                                         false, jtype, jscrollAmount, jwheelRotation);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// send out the appropriate event
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
}


void
ComponentAdapter::MenuItemInvoked(BMessage * message)
{
	void * pointer = NULL;
	if (message->FindPointer(AWT_MENU_ITEM_ADAPTER_POINTER, &pointer) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	MenuItemAdapter * adapter = dynamic_cast<MenuItemAdapter*>((ObjectAdapter*)pointer);
	if (adapter == NULL) {
		fprintf(stdout, "%s: invalid menu item adapter\n", __PRETTY_FUNCTION__);
		return;
	}
	adapter->Invoked(Environment(), message);
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
ComponentAdapter::MessageReceived(BMessage * message)
{
	if (IsUselessMessage(message)) {
		return;
	}
	fprintf(stdout, "%s:", __PRETTY_FUNCTION__);
	fprint_message(stdout, message);
	fprintf(stdout, "\n");
	switch (message->what) {
	case B_MODIFIERS_CHANGED:
		ModifiersChanged();
		break;
	case B_MOUSE_WHEEL_CHANGED: 
		MouseWheelChanged();
		break;
	case AWT_MENU_ITEM_INVOKED:
		MenuItemInvoked(message);
		break;
	case B_KEY_DOWN:
	case B_KEY_UP:
	case B_UNMAPPED_KEY_DOWN: 
	case B_UNMAPPED_KEY_UP: 
		FireKeyboardEvent();
		break;
	case B_CONTROL_INVOKED:
		InvocationReceived(message);
		break;
	case B_CONTROL_MODIFIED:
		SelectionReceived(message);
		break;
	case B_REFS_RECEIVED:
		RefsReceived(message);
		break;
	case B_SAVE_REQUESTED:
		SaveReceived(message);
		break;
	case B_CANCEL:
		CancelReceived(message);
		break;
	case AWT_FOCUS_REQUESTED: {
		// The GlobalRef of the lightweightChild is cleaned up by the NativelyFocus() call.
		JNIEnv *jenv = Environment()->env;
		KeyboardFocusManager::GetInstance()->NativelyFocus(jenv, this, message);
		break;
	}
	default:
		fprintf(stdout, "ComponentAdapter::UNHANDLED: ");
		message->PrintToStream();
		fprintf(stdout, "\n");
		break;
	}
}


/* virtual */ void
ComponentAdapter::MakeFocus(bool state = true) {
	// TODO: FocusEvent emitting may be better off in KeyboardFocusManager, but I doubt it.
	// Eitherway, the way the JNIEnv gets passed around needs looked at closer.
	
	DASSERT(GetView() != NULL);

	if (GetView()->LockLooper()) {
		if (state != GetView()->IsFocus()) {
			if (state) {
				GetView()->SetFlags(GetView()->Flags() | B_NAVIGABLE);
				GetView()->MakeFocus(true);
				KeyboardFocusManager::GetInstance()->SetFocusedComponent(this);
			} else {
				GetView()->MakeFocus(false);
				GetView()->SetFlags(GetView()->Flags() & ~B_NAVIGABLE);
			}
		} 
		GetView()->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}

	JNIEnv *env = GetEnv();
	jint type = (state ? java_awt_event_FocusEvent_FOCUS_GAINED
	                   : java_awt_event_FocusEvent_FOCUS_LOST);
	jclass FocusEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/event/FocusEvent"));
	jmethodID FocusEventConstructor_ID
		= env->GetMethodID(FocusEventClass_ID, "<init>",
		                   "(Ljava/awt/Component;IZLjava/awt/Component;)V");

	jobject target = GetTarget(env);
	DASSERT(target != NULL);
	jobject event = env->NewObject(FocusEventClass_ID, FocusEventConstructor_ID,
	                      target, type, JNI_FALSE, NULL);
	
	jclass SequencedEventClass_ID
		= (jclass)env->NewGlobalRef(env->FindClass("java/awt/SequencedEvent"));
	jmethodID SequencedEventConstructor_ID
		= env->GetMethodID(SequencedEventClass_ID, "<init>",
		                   "(Ljava/awt/AWTEvent;)V");

	jobject seqEvent = env->NewObject(SequencedEventClass_ID, SequencedEventConstructor_ID, event);
	SendEvent(seqEvent);
	
	env->DeleteLocalRef(seqEvent);
	env->DeleteLocalRef(event);
	env->DeleteLocalRef(target);
}


/* virtual */ void
ComponentAdapter::MouseDown(BPoint point)
{
	FireMouseEvent();
}


/* virtual */ void
ComponentAdapter::MouseUp(BPoint point)
{
	FireMouseEvent();
}


/* virtual */ void
ComponentAdapter::MouseMoved(BPoint point, uint32 code, const BMessage * message)
{
	FireMouseEvent();
}


/* virtual */ void
ComponentAdapter::WindowActivated(bool state)
{
	PRINT_PRETTY_FUNCTION();
}


/* virtual */ void
ComponentAdapter::KeyDown(const char * bytes, int32 numBytes)
{
	FireKeyboardEvent();
}


/* virtual */ void
ComponentAdapter::KeyUp(const char * bytes, int32 numBytes)
{
	FireKeyboardEvent();
}


/* virtual */ void
ComponentAdapter::InvocationReceived(BMessage * message)
{
	fprintf(stdout, "ERROR: missing %s version of %s\n", typeid(*this).name(), __func__);
	message->PrintToStream();
}


/* virtual */ void
ComponentAdapter::SelectionReceived(BMessage * message)
{
	fprintf(stdout, "ERROR: missing %s version of %s\n", typeid(*this).name(), __func__);
	message->PrintToStream();
}


/* virtual */ void
ComponentAdapter::RefsReceived(BMessage * message)
{
	fprintf(stdout, "ERROR: missing %s version of %s\n", typeid(*this).name(), __func__);
	message->PrintToStream();
}


/* virtual */ void
ComponentAdapter::SaveReceived(BMessage * message)
{
	fprintf(stdout, "ERROR: missing %s version of %s\n", typeid(*this).name(), __func__);
	message->PrintToStream();
}


/* virtual */ void
ComponentAdapter::CancelReceived(BMessage * message)
{
	fprintf(stdout, "ERROR: missing %s version of %s\n", typeid(*this).name(), __func__);
	message->PrintToStream();
}


// #pragma mark -
//
// surface functions
//

// Invokes "handleExpose" on our java peer object, sending a paint event
void 
ComponentAdapter::HandleExpose(JNIEnv * jenv, BRect * clip)
{
	BRect offset = *clip;
	offset.OffsetBy(GetInsets().LeftTop());
	JNU_CallMethodByName(jenv, NULL, GetPeer(jenv), "handleExpose",
		"(IIII)V", (jint)offset.left, (jint)offset.top,
		(jint)offset.Width(), (jint)offset.Height());
}


/* virtual */ void
ComponentAdapter::CreateSurface(BRect bounds, bool sendExpose) {
	DASSERT(surface == NULL);
	surface = new BBitmap(bounds, B_RGBA32, false, true);
	if (ShouldClearSurface()) {
		uint8 * ybits = (uint8*)surface->Bits();
		int32 delta_ybits = surface->BytesPerRow();
		int w = bounds.IntegerWidth();
		int h = bounds.IntegerHeight();
		for (int y = 0 ; y < h ; y++) {
			uint32 * xbits = (uint32*)ybits;
			for (int x = 0 ; x < w ; x++) {
				(*xbits++) = B_TRANSPARENT_MAGIC_RGBA32;
			}
			ybits += delta_ybits;
		}
	}
	if (sendExpose) {
		HandleExpose((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), &bounds);
	}
}


// RecreateSurface re-creates the BBitmap Surface Data raster for the component,
// if the current bitmap is not suitable.
/* virtual */ void
ComponentAdapter::RecreateSurface(bool sendExpose)
{
	DASSERT(surface != NULL);
	if (view->LockLooper()) {
		BRect bounds = view->Bounds();
		if (bounds != surface->Bounds()) {
			delete surface;
			surface = NULL;
			CreateSurface(bounds, sendExpose);
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ComponentAdapter::RepaintView(BRect rect)
{
	rect.OffsetBy(GetInsets().LeftTop());
	if (view->LockLooper()) {
		view->Draw(rect);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


/**
 * This method should only be called by the native components Draw().
 * 
 * Take heed, if called from elsewhere, it requires the fView's BLooper
 * to be locked prior to invoking.
 */
void
ComponentAdapter::DrawSurface(BRect rect)
{
	// clamp to zero
	rect.left = max_c(0, rect.left);
	rect.top = max_c(0, rect.top);
	if (surface != NULL && !view->IsHidden()) {
		drawing_mode defMode = view->DrawingMode();
		view->SetDrawingMode(B_OP_ALPHA);
		view->DrawBitmap(surface, rect, rect);
		view->SetDrawingMode(defMode);
	}
}


/* virtual */ BRect
ComponentAdapter::GetInsets()
{
	static BRect noInsets(0, 0, 0, 0);
	return noInsets;
}

		
// #pragma mark -
//
// JNI
//

jfieldID ComponentAdapter::peer_ID       = NULL;
jfieldID ComponentAdapter::privateKey_ID = NULL;
jfieldID ComponentAdapter::x_ID          = NULL;
jfieldID ComponentAdapter::y_ID          = NULL;
jfieldID ComponentAdapter::width_ID      = NULL;
jfieldID ComponentAdapter::height_ID     = NULL;

#include "java_awt_Component.h"

/*
 * Class:     java_awt_Component
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Component_initIDs
  (JNIEnv * jenv, jclass klass)
{
	ComponentAdapter::peer_ID       = jenv->GetFieldID(klass, "peer", "Ljava/awt/peer/ComponentPeer;");
	ComponentAdapter::privateKey_ID = jenv->GetFieldID(klass, "privateKey", "Ljava/lang/Object;");
	ComponentAdapter::x_ID          = jenv->GetFieldID(klass, "x", "I");
	ComponentAdapter::y_ID          = jenv->GetFieldID(klass, "y", "I");
	ComponentAdapter::width_ID      = jenv->GetFieldID(klass, "width", "I");
	ComponentAdapter::height_ID     = jenv->GetFieldID(klass, "height", "I");
	DASSERT(ComponentAdapter::peer_ID       != NULL);
	DASSERT(ComponentAdapter::privateKey_ID != NULL);
	DASSERT(ComponentAdapter::x_ID          != NULL);
	DASSERT(ComponentAdapter::y_ID          != NULL);
	DASSERT(ComponentAdapter::width_ID      != NULL);
	DASSERT(ComponentAdapter::height_ID     != NULL);
}
