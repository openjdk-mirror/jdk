#include "AwtEvent.h"
#include "EventEnvironment.h"
#include "Insets.h"
#include "KeyboardFocusManager.h"
#include "WindowAdapter.h"
#include "Window.h"
#include "RootView.h"
#include "java_awt_Frame.h"
#include "java_awt_event_ComponentEvent.h"
#include <View.h>
#include <ScrollView.h>


static BView * 
get_main_view(BWindow * window) {
	BView * view = NULL;
	assert(window->Lock());
	view = window->ChildAt(0);
	window->Unlock();
	return view;
}


/* static */ BWindow * 
WindowAdapter::NewWindow(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	frame.right = max_c(frame.left + 10, frame.right);
	frame.bottom = max_c(frame.top + 10, frame.bottom);
	BWindow * window = new Window(frame, NULL,
		B_NO_BORDER_WINDOW_LOOK, B_FLOATING_ALL_WINDOW_FEEL);
	return window;
}


WindowAdapter::WindowAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BWindow * window) 
	: ContainerAdapter(jenv, jpeer, jparent, get_main_view(window)),
	  window(NULL), state(java_awt_Frame_NORMAL), iconified(false)
{
	this->window = window;
	if (window->Look() == B_DOCUMENT_WINDOW_LOOK) {
		// must be FileDialog, nothing to do!
		return;
	}
	DASSERT(window->Look() == B_NO_BORDER_WINDOW_LOOK || // Window
	        window->Look() == B_TITLED_WINDOW_LOOK);     // Frame or Dialog
	UpdateInsets();
	Window * adaptableWindow = dynamic_cast<Window*>(window);
	if (adaptableWindow != NULL) {
		adaptableWindow->SetAdapter(this);
	}
	if (window->LockLooper()) {
		window->AddHandler(this);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	// Start the windows's looper thread.
	window->Hide(); // since hide & show stack, stay hidden after we show
	window->Show();
	// Make sure all subclasses of WindowAdapter lock window before they 
	// do anything to window in their own constructor.
}


WindowAdapter::~WindowAdapter()
{
	DASSERT(window == NULL);
}


// #pragma mark -
//
// JNI entry points
//


void
WindowAdapter::hide()
{
	if (window->Lock()) {
		while (!window->IsHidden()) {
			window->Hide();
		}
		window->Unlock();
	}
}


void
WindowAdapter::show()
{
	if (window->Lock()) {
		while (window->IsHidden()) {
			window->Show();
		}
		GetView()->MakeFocus(true);
		window->Unlock();
	}
}


void
WindowAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	// Andrew: this implementation is highly questionable IMHO
	while (window->IsLocked()) {
		window->Unlock();
	}
}


void
WindowAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	// Andrew: this implementation is highly questionable IMHO
	window->Lock();
}


void
WindowAdapter::updateWindow()
{
	window->Sync();
	if (window->NeedsUpdate()) {
		window->UpdateIfNeeded();
	}
}


void
WindowAdapter::dispose()
{
	if (window->LockLooper()) {
		window->RemoveHandler(this);
		window->Quit();
		window = NULL;
	} else {
		LOCK_LOOPER_FAILED();
	}
	ComponentDeleted();
	delete this;
}


BPoint
WindowAdapter::getLocationOnScreen()
{
	BPoint point = window->Frame().LeftTop();
	point -= GetInsets().LeftTop();
	return point;
}


void
WindowAdapter::nativeHandleEvent(jobject awtEvent)
{
	JNIEnv * jenv = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	jint eventId = jenv->GetIntField(awtEvent, AwtEvent::id_ID);
	
	if (eventId == java_awt_event_ComponentEvent_COMPONENT_RESIZED) {
		RecreateSurface(false);
	}
	return;
}


void
WindowAdapter::layoutBeginning()
{
	// Andrew: this is called on the outside-most Containers.  as such, it
	//         is a good place to do smart things such as:
	// Andrew: BWindow: DisableUpdates(), BeginViewTransaction()
	// Andrew: fyysik: BeginViewTransaction didn't have any effect when I tried it
	// Andrew: per Be Newsletter, Volume II, Issue 45:
	//         BWindow::BeginViewTransaction() and BWindow::EndViewTransaction()
	//         do implicit DisableUpdates() and EnableUpdates().
}


void
WindowAdapter::layoutEnded()
{
	UpdateDragger();
	// Andrew: this is called on the outside-most Containers.  as such, it
	//         is a good place to do smart things such as:
	// Andrew: BWindow: EnableUpdates(), EndViewTransaction(), Flush(), Sync()
}


bool
WindowAdapter::isPaintPending()
{
	return window->NeedsUpdate();
}


void
WindowAdapter::toFront()
{
	show();
}


void
WindowAdapter::toBack()
{
	window->SendBehind(window);
}


void
WindowAdapter::setResizable(bool resizable)
{
	if ((window->Flags() & B_NOT_RESIZABLE) && resizable) {
		window->SetFlags(window->Flags() ^ B_NOT_RESIZABLE);
	} else if (!resizable) {
		window->SetFlags(window->Flags() | B_NOT_RESIZABLE);
	}
	if ((window->Flags() & B_NOT_ZOOMABLE) && resizable) {
		window->SetFlags(window->Flags() ^ B_NOT_ZOOMABLE);
	} else if (!resizable) {
		window->SetFlags(window->Flags() | B_NOT_ZOOMABLE);
	}
}


void
WindowAdapter::setTitle(const char * title)
{
	window->SetTitle(title);
}


void
WindowAdapter::setUndecorated(bool undecorated) {
	if (undecorated) {
		if (window->Look() == B_NO_BORDER_WINDOW_LOOK) {
			// nothing to do
			return;
		}
		window->SetLook(B_NO_BORDER_WINDOW_LOOK);
	} else {
		if (window->Look() != B_NO_BORDER_WINDOW_LOOK) {
			// nothing to do
			return;
		}
		window->SetLook(B_TITLED_WINDOW_LOOK);
		UpdateDragger();
	}
	UpdateInsets();
}


/* static */ float
WindowAdapter::getMinimumWidth()
{
	float width = 40; // = 30 + 5 + 5 [for titled window]
	return width;
}


/* static */ float
WindowAdapter::getMinimumHeight()
{
	float height = 62; // = 33 + 5 + 5 + 19 [for titled window]
	return height;
}


void
WindowAdapter::reshapeFrame(int x, int y, int width, int height)
{
	if (window->Look() == B_NO_BORDER_WINDOW_LOOK) {
		width = max_c(width, 10);
		height = max_c(height, 10);
	} else {
		width = max_c(width, (int)getMinimumWidth());
		height = max_c(height, (int)getMinimumHeight());
	}
	float window_x = x + insets.left;
	float window_y = y + insets.top;
	float window_width  = width  - insets.left - insets.right;
	float window_height = height - insets.top - insets.bottom;
	BView * view = GetView();
	if (view->LockLooper()) {
		BRect frame = window->Frame();
		if ((frame.left != window_x) || (frame.top != window_y)) {
			window->MoveTo(window_x, window_y);
		}
		if ((frame.Width() != window_width) || (frame.Height() != window_height)) {
			window->ResizeTo(window_width, window_height);
			view->ResizeTo(width, height);
			RecreateSurface(false);
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


int
WindowAdapter::getScreenImOn()
{
	if (window->Workspaces() != B_ALL_WORKSPACES) {
		return window->Workspaces();
	}
	// Hrm....
	return 0;
}

// #pragma mark -
//
// Haiku entry points
//

// Sends an java/awt/event/WindowEvent from the current peer object,
// also handles setting the flags for the window.
void
WindowAdapter::SendWindowEvent(jint eventID)
{
	EventEnvironment * environment = Environment();
	JNIEnv * env = environment->env;
	jint oldState = GetState();
	jint newState = oldState;
	
	switch (eventID) {
	case java_awt_event_WindowEvent_WINDOW_ICONIFIED:
		newState = oldState | java_awt_Frame_ICONIFIED;
		break;
	case java_awt_event_WindowEvent_WINDOW_DEICONIFIED:
		if (oldState & java_awt_Frame_ICONIFIED) {
			newState = oldState ^ java_awt_Frame_ICONIFIED;
		}
		break;
	default:
		break;
	}
	// Set the updated state on the native peer object.
	if (newState != oldState) {
		SetState(newState);
	}
	
	// Grab a ref to the peer's java.awt target class.
	jobject jtarget = GetTarget(env);
	jobject opposite = NULL;
	
	// Create the event
	jobject event = environment->NewWindowEvent(jtarget, eventID,
					opposite, oldState, newState);
	
	// Clean up the local reference to the peer's target.
	env->DeleteLocalRef(jtarget);
	jtarget = NULL;
	
	// If this is a Focus Gained or Focus Lost we wrap it in a 
	// sequencedEvent, and replace the original event object with 
	// the sequenceEvent.
	if (eventID == java_awt_event_WindowEvent_WINDOW_GAINED_FOCUS ||
		eventID == java_awt_event_WindowEvent_WINDOW_LOST_FOCUS)
	{
		jobject sequencedEvent = environment->NewSequencedEvent(event);
		env->DeleteLocalRef(event);
		event = sequencedEvent;
	}
	SendEvent(event);
	
	// Don't forget to clean up!
	env->DeleteLocalRef(event);
}


/* virtual */ void
WindowAdapter::FrameMoved(BPoint point)
{
	point -= GetInsets().LeftTop();
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jtarget = GetTarget(jenv);
	jobject jevent = environment->NewComponentEvent(jtarget, java_awt_event_ComponentEvent_COMPONENT_MOVED);
	jenv->SetIntField(jtarget, x_ID, point.x);
	jenv->SetIntField(jtarget, y_ID, point.y);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
}


/* virtual */ void
WindowAdapter::FrameResized(float width, float height)
{
	BRect insets = GetInsets();
	float frameWidth = width + insets.left + insets.right;
	float frameHeight = height + insets.top + insets.bottom;
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jtarget = GetTarget(jenv);
	jint targetWidth = jenv->GetIntField(jtarget, width_ID);
	jint targetHeight = jenv->GetIntField(jtarget, height_ID);
	if ((frameWidth != targetWidth) || (frameHeight != targetHeight)) {
		jobject jevent = environment->NewComponentEvent(jtarget, java_awt_event_ComponentEvent_COMPONENT_RESIZED);
		jenv->SetIntField(jtarget, width_ID,  frameWidth);
		jenv->SetIntField(jtarget, height_ID, frameHeight);
		SendEvent(jevent);
		jenv->DeleteLocalRef(jevent);
		jevent = NULL;
	}
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
}


/* virtual */ void
WindowAdapter::WindowActivated(bool state)
{
	if (state) {
		// R5 doesn't seem to fire deiconifications. So we fire them here.
		// When a window is un-minimized, it gets activated immediately.
		// If post R5 versions fire the B_MINIMIZE with a minimized = false
		// Prior to sending B_ACTIVATED we'll be sending it there instead.
		if (iconified) {
			iconified = false;
			SendWindowEvent(java_awt_event_WindowEvent_WINDOW_DEICONIFIED);
		}
		SendWindowEvent(java_awt_event_WindowEvent_WINDOW_GAINED_FOCUS);
		SendWindowEvent(java_awt_event_WindowEvent_WINDOW_ACTIVATED);
	} else {
		SendWindowEvent(java_awt_event_WindowEvent_WINDOW_LOST_FOCUS);
		SendWindowEvent(java_awt_event_WindowEvent_WINDOW_DEACTIVATED);
	}
}


// note: this doesn't get announced if you minimize by "hide all" on deskbar
/* virtual */ void
WindowAdapter::Minimize(bool minimize)
{
	PRINT_PRETTY_FUNCTION();
	if (minimize) {
		SendWindowEvent(java_awt_event_WindowEvent_WINDOW_ICONIFIED);
	} else if (iconified) {
		// R5 never fires this branch, post R5 may. In that case, only send
		// deiconified here if it comes before the B_UPDATE we used to
		// fire these messages under R5.
		SendWindowEvent(java_awt_event_WindowEvent_WINDOW_DEICONIFIED);
	}
	iconified = minimize;
}


/* virtual */ void
WindowAdapter::Zoom(BPoint position, float width, float height)
{
	TODO();
}


/* virtual */ void
WindowAdapter::QuitRequested()
{
	SendWindowEvent(java_awt_event_WindowEvent_WINDOW_CLOSING);
}


// #pragma mark -
//
// Haiku entry points end
//

void
WindowAdapter::AttachChild(BView * child) {
	ContainerAdapter::AttachChild(child);
	UpdateDragger();
}


void
WindowAdapter::DetachChild(BView * child) {
	ContainerAdapter::DetachChild(child);
	UpdateDragger();
}


/*
 * This somewhat complicated function updates the dragger in the
 * corner of document views so that it shows the dragger that fits
 * neatly into a scrollable view.  Also, if the scrollable view
 * is removed, it removes the document dragger.
 */
void
WindowAdapter::UpdateDragger()
{
	if (window->LockLooper()) {
		BPoint corner = window->Bounds().RightBottom() - 
		                BPoint(B_V_SCROLL_BAR_WIDTH/2.0, B_H_SCROLL_BAR_HEIGHT/2.0);
		BView * root = window->ChildAt(0);
		int i = 0;
		BView * view = NULL;
		bool document = false;
		while ((view = root->ChildAt(i++)) != NULL) {
			if (!view->Frame().Contains(corner)) {
				continue;
			}
			BScrollView * scroll = dynamic_cast<BScrollView*>(view);
			if (scroll == NULL) {
				root = view;
				corner -= view->Frame().LeftTop();
				i = 0;
				continue;
			}
			if ((scroll->ScrollBar(B_HORIZONTAL) != NULL) &&
				(scroll->ScrollBar(B_VERTICAL) != NULL)) {
				if (window->Look() == B_TITLED_WINDOW_LOOK) {
					window->SetLook(B_DOCUMENT_WINDOW_LOOK);
				}
				document = true;
				break;
			}
		}
		if (!document && (window->Look() == B_DOCUMENT_WINDOW_LOOK)) {
			// it's no longer a document, reset it to titled
			window->SetLook(B_TITLED_WINDOW_LOOK);
		}
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
WindowAdapter::DrawSurface(BRect rect)
{
	BView * view = GetView();
	BBitmap * surface = GetSurface();
	if (surface != NULL && !view->IsHidden()) {
//		BRect bounds = view->Bounds();
//		fprintf(stdout, "DrawSurface(%.0f,%.0f-%.0f,%.0f)\t: View(%.0fx%.0f)\n",
//		        rect.left, rect.top, rect.right, rect.bottom,
//		        bounds.right, bounds.bottom);
		// clamp to zero
		rect.left = max_c(0, rect.left);
		rect.top = max_c(0, rect.top);
		drawing_mode defMode = view->DrawingMode();
		view->SetDrawingMode(B_OP_COPY);
		view->DrawBitmap(surface, rect, rect);
		view->SetDrawingMode(defMode);
	}
}


void
WindowAdapter::UpdateInsets()
{
	int left = 0, top = 0, right = 0, bottom = 0;
	if (window->Look() != B_NO_BORDER_WINDOW_LOOK) {
		// Frame or Dialog
		left = right = bottom = 5; top = 24;
	}

	// update the window
	BView * view = GetView();
	if (view->LockLooper()) {
		float oldLeft = -view->Frame().left;
		float oldTop = -view->Frame().top;
		if ((oldLeft != left) || (oldTop != top)) {
			view->MoveBy(oldLeft-left, oldTop-top);
			window->MoveBy(left-oldLeft, top-oldTop);
			window->ResizeTo(view->Bounds().Width() - left - right,
			                 view->Bounds().Height() - top - bottom);
		}
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}

	// update our cache, which doesn't believe in the menubar offset
	insets.left = left;
	insets.top = top;
	insets.right = right;
	insets.bottom = bottom;

	// Add the menu bar inset (if any) after updating the window,
	// since we don't want to offset the window by menubar height.
	if (window->Look() != B_NO_BORDER_WINDOW_LOOK) {
		if (window->LockLooper()) {
			if (window->KeyMenuBar() != NULL) {
				top += 20;
			}
			window->UnlockLooper();
		} else {
			LOCK_LOOPER_FAILED();
		}
	}

	// update java
	JNIEnv *env = (JNIEnv*)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	jobject peer = GetPeer(env);
	jobject peerInsets = env->GetObjectField(peer, ContainerAdapter::insets_ID);
	if (peerInsets != NULL) {
		env->SetIntField(peerInsets, Insets::top_ID,    top);
		env->SetIntField(peerInsets, Insets::bottom_ID, bottom);
		env->SetIntField(peerInsets, Insets::left_ID,   left);
		env->SetIntField(peerInsets, Insets::right_ID,  right);
		env->DeleteLocalRef(peerInsets);
	} else {
		jobject jinsets = Insets::New(env, insets);
		if (jinsets != NULL) {
			env->SetObjectField(peer, ContainerAdapter::insets_ID, jinsets);
			env->DeleteLocalRef(jinsets);
		}
	}
}


jint
WindowAdapter::GetState() {
	return state;
}


void
WindowAdapter::SetState(jint state) {
	this->state = state;
}



// #pragma mark -
//
// JNI
//

#include "java_awt_Window.h"

/*
 * Class:     java_awt_Window
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Window_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}
