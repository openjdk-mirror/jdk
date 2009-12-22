#include "Window.h"
#include "WindowAdapter.h"
#include "EventEnvironment.h"
#include "RootView.h"
#include <ByteOrder.h>
#include <Message.h>
#include <String.h>
#include <View.h>

static BRect
compute_window_frame(BRect frame, window_look look)
{
	if (look == B_TITLED_WINDOW_LOOK) {
		frame.InsetBy(5, 5);
		frame.top += 19;
		DASSERT(frame.right - frame.left >= 30);
		DASSERT(frame.bottom - frame.top >= 33);
	} else {
		DASSERT(look == B_NO_BORDER_WINDOW_LOOK);
		DASSERT(frame.right - frame.left >= 10);
		DASSERT(frame.bottom - frame.top >= 10);
	}
	return frame;
}


Window::Window(BRect frame, const char * title,
               window_look look, window_feel feel)
	: BDirectWindow(compute_window_frame(frame, look), title, look, feel,
	                B_ASYNCHRONOUS_CONTROLS, B_CURRENT_WORKSPACE),
	  adapter(NULL), environment(NULL) 
{
	fConnected = false;
	fPrevClip.MakeEmpty();
	BView * view = new RootView(Bounds());
	AddChild(view);
	if (look == B_TITLED_WINDOW_LOOK) {
		view->MoveTo(-5, -24);
		view->ResizeTo(frame.Width(), frame.Height());
	}
}


Window::~Window()
{
	DASSERT(environment == NULL);
	adapter = NULL; // for safety purposes
}


void
Window::SetAdapter(WindowAdapter * adapter)
{
	this->adapter = adapter;
	if (LockLooper()) {
		RootView * view = dynamic_cast<RootView*>(ChildAt(0));
		view->SetAdapter(adapter);
		UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}

// #pragma mark -
//
// Utility functions
//

void
Window::SetupEnvironment()
{
	JNIEnv * env;
	jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL);
	environment = new EventEnvironment(env);
}


void
Window::ReleaseEnvironment()
{
	delete environment;
	environment = NULL;
	JNIEnv * env;
	jvm->DetachCurrentThread();
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
Window::DispatchMessage(BMessage *message, BHandler *handler)
{
	if (environment == NULL) {
		SetupEnvironment();
	}
	Window * window = dynamic_cast<Window*>(handler);
	if (window != NULL) {
		DASSERT(window == this);
		switch (message->what) {
		case _QUIT_: {
			// special case of "quit" requires us to release the
			// environment and detach this thread before it dies.
			ReleaseEnvironment();
			break;
		}
		case B_QUIT_REQUESTED: {
			// special case of "quit requested" requires returning
			// from DispatchMessage to block the default behavior
			// of closing the window
			adapter->QuitRequested();
			return;
		}
		case _UPDATE_: {
			// special case of "update" requires catching in
			// dispatch message since it doesn't fall out in the
			// normal MessageReceived or any other hook functions
			BRect clip = Bounds();
			// Update is synonymous with Expose. It only fires when a new area is exposed
			// Or an area is dirtied (painted over).
			// Weather that's through unobstruction of the window or by a resize, it's the
			// same. It's an expose.
			adapter->HandleExpose(environment->env, &clip);
			break;
		}
		default:
			break;
		}
	}
	BDirectWindow::DispatchMessage(message, handler);
}


/* virtual */ void
Window::MessageReceived(BMessage * message)
{
	if (IsUselessMessage(message)) {
		BDirectWindow::MessageReceived(message);
		return;
	}
	fprintf(stdout, "%s:", __PRETTY_FUNCTION__);
	fprint_message(stdout, message);
	fprintf(stdout, "\n");
	BDirectWindow::MessageReceived(message);
	adapter->MessageReceived(message);
}


/* virtual */ void
Window::FrameMoved(BPoint point)
{
	BDirectWindow::FrameMoved(point);
	adapter->FrameMoved(point);
}


/* virtual */ void
Window::FrameResized(float width, float height)
{
	BDirectWindow::FrameResized(width, height);
	adapter->FrameResized(width, height);
}


/* virtual */ void
Window::Minimize(bool minimize)
{
	PRINT_PRETTY_FUNCTION();
	BDirectWindow::Minimize(minimize);
	adapter->Minimize(minimize);
}


/* virtual */ void
Window::WindowActivated(bool state)
{
	BDirectWindow::WindowActivated(state);
	adapter->WindowActivated(state);
}


/* virtual */ void
Window::Zoom(BPoint position, float width, float height)
{
	PRINT_PRETTY_FUNCTION();
	BDirectWindow::Zoom(position, width, height);
	adapter->Zoom(position, width, height);
}

	
/* virtual */ void
Window::MenusBeginning()
{
	BDirectWindow::MenusBeginning();
	// TODO: anything to do here?
	return;
}


/* virtual */ void
Window::MenusEnded()
{
	BDirectWindow::MenusEnded();
	// TODO: anything to do here?
	return;
}


/* virtual */ void
Window::WorkspaceActivated(int32 workspaces, bool state)
{
	PRINT_PRETTY_FUNCTION();
	BDirectWindow::WorkspaceActivated(workspaces, state);
}


/* virtual */ void
Window::WorkspacesChanged(uint32 old_workspaces, uint32 new_workspaces)
{
//	fprintf(stdout, "%s(%ld,%ld)\n", __func__, old_workspaces, new_workspaces);
	BDirectWindow::WorkspacesChanged(old_workspaces, new_workspaces);
	// TODO: Once the GFX can handle multiple colorspaces....
	// Recreate the surface data of all components to match colorspace of
	// the new_workspace. Revalidate the entire tree.
}


/* virtual */ void
Window::ScreenChanged(BRect screen_size, color_space depth)
{
	PRINT_PRETTY_FUNCTION();
	BDirectWindow::ScreenChanged(screen_size, depth);
	// TODO: Once the GFX can handle multiple colorspaces....
	// Recreate the surface data of all components to match the
	// colorspace depth. Revalidate the entire tree.
}

//
// BDirectWindow
//

void
Window::DirectConnected(direct_buffer_info *info) {
	if (info->buffer_state & B_DIRECT_STOP) {
		fConnected = false;
		fPrevClip.MakeEmpty();
		return;
	}

#ifndef BDirectWindow	
	// This could get hairy...
	BRegion curClip;
	if (GetClippingRegion(&curClip) == B_OK) {
		BRegion workReg = curClip;
		workReg.Exclude(&fPrevClip);
		if (workReg.Frame().IsValid()) {
			// At this point, workReg = the newly exposed areas that will need
			// re-blit from existing memory spaces (when BUFFER_MOVED) or 
			// freshly drawn when BUFFER_RESIZED. 
			// At any rate, we should be able to fire window events
			// my posting the appropriate message event queue here.
			// We'll also be able to provide more detailed information in our
			// own messages, which should make graphics repaints faster.
		}
		fPrevClip = curClip;
	}
#endif // BDirectWindow	
	
	if (info->buffer_state & B_DIRECT_START) {
		fConnected = true;
	} else if (info->buffer_state & B_DIRECT_MODIFY) {
		if (info->buffer_state & B_BUFFER_MOVED) {
			
		} else if (info->buffer_state & B_BUFFER_RESET) {
			
		} else if (info->buffer_state & B_BUFFER_RESIZED) {
		
		} else if (info->buffer_state & B_CLIPPING_MODIFIED) {
			
		}
	}
}
