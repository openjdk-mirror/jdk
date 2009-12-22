#include "FileDialogAdapter.h"
#include <app/Application.h>
#include <interface/View.h>
#include <interface/Window.h>
#include <storage/FilePanel.h>
#include <storage/Path.h>
#include <support/String.h>
#include "java_awt_FileDialog.h"
#include "EventEnvironment.h"

/* static */ BFilePanel * 
FileDialogAdapter::NewFilePanel(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	int jmode = jenv->GetIntField(jjavaObject, mode_ID);
	file_panel_mode mode = (jmode == java_awt_FileDialog_SAVE ? B_SAVE_PANEL : B_OPEN_PANEL);
	BFilePanel * panel = new BFilePanel(mode, NULL, NULL, 0, false);
	return panel;
}


FileDialogAdapter::FileDialogAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BFilePanel * filePanel)
 	: DialogAdapter(jenv, jpeer, jparent, filePanel->Window()),
	  filter(NULL), waitingForMessage(false), environment(NULL)
{
	this->filePanel = filePanel;
	BWindow * window = GetWindow();
	if (window->LockLooper()) {
		window->AddHandler(this);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	filePanel->SetTarget(this);
}


FileDialogAdapter::~FileDialogAdapter()
{
	DASSERT(filePanel == NULL);
	DASSERT(filter == NULL);
}


// #pragma mark -
//
// utility functions
//

EventEnvironment * 
FileDialogAdapter::Environment()
{
	if (environment == NULL) {
		environment = new EventEnvironment(GetEnv());
	}
	return environment;
}


void
FileDialogAdapter::SetModal(bool modal)
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

void
FileDialogAdapter::show()
{
	waitingForMessage = true;
	DialogAdapter::show();
}


void
FileDialogAdapter::dispose()
{
	BWindow * window = GetWindow();
	if (window->LockLooper()) {
		window->RemoveHandler(this);
		window->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	delete filePanel;
	filePanel = NULL;
	delete filter;
	filter = NULL;
	WindowDeleted();
	ComponentDeleted();
	delete this;
	// do _not_ call the parent dispose
}


void
FileDialogAdapter::setFile(char * file)
{
	if (filePanel->PanelMode() == B_SAVE_PANEL) {
		filePanel->SetSaveText(file);
	} else {
		// just ignore it
	}
}


void
FileDialogAdapter::setDirectory(char * directory)
{
	filePanel->SetPanelDirectory(directory);
}


void
FileDialogAdapter::setFilenameFilter(BRefFilter * filter)
{
	filePanel->SetRefFilter(filter);
	delete this->filter;
	this->filter = filter;
}


// #pragma mark -
//
// Haiku entry points
//

/* virtual */ void
FileDialogAdapter::RefsReceived(BMessage * message)
{
	DASSERT(waitingForMessage);
	entry_ref ref;
	if (message->FindRef("refs", 0, &ref) != B_OK) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	BPath path(&ref);
	const char * file = path.Leaf();
	BPath dirpath;
	path.GetParent(&dirpath);
	const char * dir  = dirpath.Path();
	// set the appropriate field
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jstring jfile = jenv->NewStringUTF(file);
	jstring jdir  = jenv->NewStringUTF(dir);
	jobject jtarget = GetTarget(jenv);
	jenv->SetObjectField(jtarget, file_ID, jfile);
	jenv->SetObjectField(jtarget, dir_ID,  jdir);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// message received
	waitingForMessage = false;
	// Note: After this message we will receive a "cancel" automatically,
	//       which will wake the thread. (see below)
}


/* virtual */ void
FileDialogAdapter::SaveReceived(BMessage * message)
{
	DASSERT(waitingForMessage);
	entry_ref ref;
	BString name;
	if ((message->FindRef("directory", 0, &ref) != B_OK) ||
	    (message->FindString("name", 0, &name) != B_OK)) {
		BAD_MESSAGE();
		message->PrintToStream();
		return;
	}
	const char * file = name.String();
	BPath dirpath(&ref);
	const char * dir = dirpath.Path();
	// set the appropriate field
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jstring jfile = jenv->NewStringUTF(file);
	jstring jdir  = jenv->NewStringUTF(dir);
	jobject jtarget = GetTarget(jenv);
	jenv->SetObjectField(jtarget, file_ID, jfile);
	jenv->SetObjectField(jtarget, dir_ID,  jdir);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
	// message received
	waitingForMessage = false;
	// Note: After this message we will receive a "cancel" automatically,
	//       which will wake the thread. (see below)
}


/* virtual */ void
FileDialogAdapter::CancelReceived(BMessage * message)
{
	EventEnvironment * environment = Environment();
	JNIEnv * jenv = environment->env;
	jobject jtarget = GetTarget(jenv);
	if (waitingForMessage) {
		// set the appropriate field
		jenv->SetObjectField(jtarget, file_ID, NULL);
		// message received
		waitingForMessage = false;
	}
	// generate component hidden event
	jobject jevent = environment->NewComponentEvent(jtarget, java_awt_event_ComponentEvent_COMPONENT_HIDDEN);
	SendEvent(jevent);
	jenv->DeleteLocalRef(jevent);
	jevent = NULL;
	// wake up the thread
	jenv->CallVoidMethod(jtarget, hideAndDisposeHandler_ID);
	jenv->DeleteLocalRef(jtarget);
	jtarget = NULL;
}


// #pragma mark -
//
// JNI
//

#include "java_awt_FileDialog.h"

jfieldID FileDialogAdapter::mode_ID = NULL;
jfieldID FileDialogAdapter::file_ID = NULL;
jfieldID FileDialogAdapter::dir_ID  = NULL;

/*
 * Class:     java_awt_FileDialog
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_FileDialog_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	FileDialogAdapter::mode_ID = jenv->GetFieldID(jklass, "mode", "I");
	FileDialogAdapter::file_ID = jenv->GetFieldID(jklass, "file", "Ljava/lang/String;");
	FileDialogAdapter::dir_ID  = jenv->GetFieldID(jklass, "dir",  "Ljava/lang/String;");
	DASSERT(FileDialogAdapter::mode_ID != NULL);
	DASSERT(FileDialogAdapter::file_ID != NULL);
	DASSERT(FileDialogAdapter::dir_ID  != NULL);
}

