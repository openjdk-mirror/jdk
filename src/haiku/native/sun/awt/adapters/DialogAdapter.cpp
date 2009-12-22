#include "DialogAdapter.h"
#include "Window.h"
#include "RootView.h"
#include <app/Application.h>
#include <interface/MenuBar.h>
#include <interface/View.h>
#include <interface/Window.h>

/* static */ BWindow * 
DialogAdapter::NewDialog(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	frame.right = max_c(frame.left + getMinimumWidth(), frame.right);
	frame.bottom = max_c(frame.top + getMinimumHeight(), frame.bottom);
	BWindow * window = new Window(frame, "Dialog",
		B_TITLED_WINDOW_LOOK, B_NORMAL_WINDOW_FEEL);
	return window;
}


DialogAdapter::DialogAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BWindow * window)
 	: WindowAdapter(jenv, jpeer, jparent, window)
{
	if (window->LockLooper()) {
		BView * view = GetView();
		view->SetViewColor(ui_color(B_PANEL_BACKGROUND_COLOR));
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


DialogAdapter::~DialogAdapter()
{
}


// #pragma mark -
//
// utility functions
//

void
DialogAdapter::SetModal(bool modal)
{
	if ((GetWindow()->Feel() & B_MODAL_SUBSET_WINDOW_FEEL) && !modal) {
		GetWindow()->SetFeel(B_NORMAL_WINDOW_FEEL);
	} else if (modal) {
		GetWindow()->SetFeel(B_MODAL_SUBSET_WINDOW_FEEL);
	}
}


// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
DialogAdapter::show()
{
	// reset the "modal" behavior of the dialog before each show()
	JNIEnv * jenv = (JNIEnv*)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	jobject jtarget = GetTarget(jenv);
	SetModal(JNU_CallMethodByName(jenv, NULL, jtarget, "isModal", "()Z").z);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	WindowAdapter::show();
}


/**
 * If we're running in modal mode, we need to remove ourself from any subsets we may be a member of.
 * If we do not do this, regardless of our visible state, we'll block our parents input in Haiku.
 */
/* virtual */ void
DialogAdapter::hide()
{
	BWindow *window;
	int32 i = 0;
	while (window = be_app->WindowAt(i++)) {
		if (GetWindow() != window) { // ignore ourself.
			GetWindow()->RemoveFromSubset(window);
		}
	}
	WindowAdapter::hide();
}

/**
 * Called by the java peer when the dialog is being shown and is modal.
 * We grab all the windows on the screen into our modal subset.
 * 
 * The java peer is responsible for removing the children of this 
 * dialog from the subset.
 */
void
DialogAdapter::createFullSubset()
{
	// Add all the existing BWindows to this Modal's subset.
	BWindow * window;
	int32 i = 0;
	while (window = be_app->WindowAt(i++)) {
		if (GetWindow() != window) { // ignore ourself.
			GetWindow()->AddToSubset(window);
		}
	}
}


/**
 * Called by the java peer when the dialog is being shown and is modal.
 * 
 * Removes a given BWindow from our subset.
 */
void
DialogAdapter::removeFromSubset(BWindow * window)
{
	GetWindow()->RemoveFromSubset(window);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Dialog.h"

/*
 * Class:     java_awt_Dialog
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Dialog_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}

