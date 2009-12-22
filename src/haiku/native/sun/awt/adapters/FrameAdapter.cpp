#include "FrameAdapter.h"
#include "Window.h"
#include "RootView.h"
#include <climits>
#include <interface/MenuBar.h>
#include <interface/View.h>
#include <interface/Window.h>

/* static */ BWindow * 
FrameAdapter::NewFrame(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	frame.right = max_c(frame.left + getMinimumWidth(), frame.right);
	frame.bottom = max_c(frame.top + getMinimumHeight(), frame.bottom);
	BWindow * window = new Window(frame, "Frame",
		B_TITLED_WINDOW_LOOK, B_NORMAL_WINDOW_FEEL);
	return window;
}


FrameAdapter::FrameAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BWindow * window)
 	: WindowAdapter(jenv, jpeer, jparent, window)
{
}


FrameAdapter::~FrameAdapter()
{
}


// #pragma mark -
//
// JNI entry points
//

// for debug purposes only
bool
hasMenubar(BWindow * window)
{
	bool hasMenuBar = false;
	if (window->LockLooper()) {
		hasMenuBar = (window->KeyMenuBar() != NULL);
		window->UnlockLooper();
	}
	return hasMenuBar;
}


/* virtual */ void
FrameAdapter::dispose()
{
	// children should already be disposed
	DASSERT(!hasMenubar(GetWindow()));
	WindowAdapter::dispose();
}


void
FrameAdapter::setMenuBar(BMenuBar * menubar) 
{
	BWindow * window = GetWindow();
	if (window->LockLooper()) {
		BMenuBar * keymenubar = window->KeyMenuBar();
		if (menubar == keymenubar) {
			// nothing to do
			window->UnlockLooper();
			return;
		}
		BView * root = GetView();
		DASSERT(root != NULL);
		if (keymenubar != NULL) {
			// remove any existing menu bar
			window->SetKeyMenuBar(NULL);
			root->RemoveChild(keymenubar);
		}
		if (menubar != NULL) {
			// We should not be attached to any other window.
			DASSERT(menubar->Window() == NULL);
			root->AddChild(menubar);
			window->SetKeyMenuBar(menubar);
			BPoint corner(0, 0);
			root->ConvertFromParent(&corner);
			menubar->MoveTo(corner);
		}
		if ((menubar != NULL) != (keymenubar != NULL)) {
			// If we removed a menu without replacing it,
			// or added a menu where none was before, we
			// need to update the insets, and recreate
			// the surface
			UpdateInsets();
		}
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
FrameAdapter::setState(int state)
{
	SetState(state);
}


int
FrameAdapter::getState()
{
	return GetState();
}


void
FrameAdapter::setMaximizedBounds(int x, int y, int w, int h)
{
	PRINT_PRETTY_FUNCTION();
	BWindow * window = GetWindow();
	if (window->LockLooper()) {
		window->SetZoomLimits(w, h);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
FrameAdapter::clearMaximizedBounds()
{
	BWindow * window = GetWindow();
	if (window->LockLooper()) {
		window->SetZoomLimits(INT_MAX, INT_MAX);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Frame.h"

/*
 * Class:     java_awt_Frame
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Frame_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}

