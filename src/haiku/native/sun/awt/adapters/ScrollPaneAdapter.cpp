#include "ScrollPaneAdapter.h"
#include "ScrollPane.h"
#include "java_awt_ScrollPane.h"

/* static */ ScrollPane *
ScrollPaneAdapter::NewScrollPane(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass javaObject as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// here we find out the scroll bar policy, and do the right thing in the constructor
	int scrollbarDisplayPolicy = jenv->GetIntField(jjavaObject, scrollbarDisplayPolicy_ID);
	bool showScrollbars = (scrollbarDisplayPolicy != java_awt_ScrollPane_SCROLLBARS_NEVER);
	BRect frame = GetFrame(jenv, jpeer);
	ScrollPane * value = new ScrollPane(frame, showScrollbars);
	return value;
}


ScrollPaneAdapter::ScrollPaneAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: ContainerAdapter(jenv, jpeer, jparent, NewScrollPane(jenv, jpeer, jparent)),
	  scrollPane(NULL)
{
	scrollPane = dynamic_cast<ScrollPane*>(GetView());
	DASSERT(scrollPane != NULL);
	scrollPane->SetAdapter(this);
}


ScrollPaneAdapter::~ScrollPaneAdapter()
{
	// TODO: delete the contentView ?
}


// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
ScrollPaneAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


/* virtual */ void
ScrollPaneAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


int
ScrollPaneAdapter::getHScrollbarHeight()
{
	BScrollBar * bar = scrollPane->ScrollBar(B_HORIZONTAL);
	if (bar == NULL) {
		return 0;
	} else if (bar->LockLooper()) {
		int height = (int)bar->Bounds().Height();
		bar->UnlockLooper();
		return height;
	} else {
		LOCK_LOOPER_FAILED();
		return 0;
	}
}


int
ScrollPaneAdapter::getVScrollbarWidth()
{
	BScrollBar * bar = scrollPane->ScrollBar(B_VERTICAL);
	if (bar == NULL) {
		return 0;
	} else if (bar->LockLooper()) {
		int width = (int)bar->Bounds().Width();
		bar->UnlockLooper();
		return width;
	} else {
		LOCK_LOOPER_FAILED();
		return 0;
	}
}


void
ScrollPaneAdapter::setScrollPosition(int x, int y)
{
	BScrollBar * horizontal = scrollPane->ScrollBar(B_HORIZONTAL);
	if (horizontal != NULL) {
		horizontal->SetValue(x);
	} else {
		DEBUGGER();
		// TODO: can we ignore this situation?
	}
	BScrollBar * vertical = scrollPane->ScrollBar(B_VERTICAL);
	if (vertical != NULL) {
		vertical->SetValue(y);
	} else {
		DEBUGGER();
		// TODO: can we ignore this situation?
	}
}


void
ScrollPaneAdapter::childResized(int width, int height)
{
	BView * contentView = scrollPane->GetContentView();
	BScrollBar * horizontal = scrollPane->ScrollBar(B_HORIZONTAL);
	if (horizontal != NULL) {
		if (horizontal->LockLooper()) {
			float maximum = width + CONTENT_VIEW_OUTSET - contentView->Bounds().Width();
			horizontal->SetRange(0, maximum);
			if (width != 0) {
				horizontal->SetProportion(contentView->Bounds().Width()/width);
			} else {
				horizontal->SetProportion(1.0);
			}
			horizontal->UnlockLooper();
		} else {
			LOCK_LOOPER_FAILED();
		}
	}
	BScrollBar * vertical = scrollPane->ScrollBar(B_VERTICAL);
	if (vertical != NULL) {
		if (vertical->LockLooper()) {
			float maximum = height + CONTENT_VIEW_OUTSET - contentView->Bounds().Height();
			vertical->SetRange(0, maximum);
			if (height != 0) {
				vertical->SetProportion(contentView->Bounds().Height()/height);
			} else {
				vertical->SetProportion(1.0);
			}
			vertical->UnlockLooper();
		} else {
			LOCK_LOOPER_FAILED();
		}
	}
}

void
ScrollPaneAdapter::setIncrements(orientation adj, int u, int b)
{
	DASSERT(adj == B_VERTICAL || adj == B_HORIZONTAL);
	BScrollBar * bar = scrollPane->ScrollBar(adj);
	if (bar != NULL) {
		bar->SetSteps(u, b);
	}
}


void
ScrollPaneAdapter::setValue(orientation adj, int v)
{
	DASSERT(adj == B_VERTICAL || adj == B_HORIZONTAL);
	BScrollBar * bar = scrollPane->ScrollBar(adj);
	if (bar != NULL) {
		bar->SetValue(v);
	}
}


// #pragma mark -
//
// heirarchy maintenance
//

void
ScrollPaneAdapter::AttachChild(BView * child) {
	BView * contentView = scrollPane->GetContentView();
	if (contentView->LockLooper()) {
		contentView->AddChild(child);
		contentView->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ScrollPaneAdapter::DetachChild(BView * child) {
	BView * contentView = scrollPane->GetContentView();
	if (contentView->LockLooper()) {
		contentView->RemoveChild(child);
		contentView->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


// #pragma mark -
//
// JNI
//

#include "java_awt_ScrollPane.h"

jfieldID ScrollPaneAdapter::scrollbarDisplayPolicy_ID = NULL;

/*
 * Class:     java_awt_ScrollPane
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_ScrollPane_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	ScrollPaneAdapter::scrollbarDisplayPolicy_ID = jenv->GetFieldID(jklass, "scrollbarDisplayPolicy", "I");
	DASSERT(ScrollPaneAdapter::scrollbarDisplayPolicy_ID);
}

#include "java_awt_ScrollPaneAdjustable.h"

/*
 * Class:     java_awt_ScrollPaneAdjustable
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_ScrollPaneAdjustable_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// nothing to do
}

