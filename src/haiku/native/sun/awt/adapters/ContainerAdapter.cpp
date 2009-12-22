#include "ContainerAdapter.h"
#include <View.h>
#include <cstdio>

ContainerAdapter::ContainerAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BView * view) 
	: ComponentAdapter(jenv, jpeer, jparent, view),
	  view(NULL)
{
	this->view = view;
}


ContainerAdapter::~ContainerAdapter()
{
}


void
ContainerAdapter::AttachChild(BView * child) {
	if (view->LockLooper()) {
		view->AddChild(child);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ContainerAdapter::DetachChild(BView * child) {
	if (view->LockLooper()) {
		view->RemoveChild(child);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
}


void
ContainerAdapter::layoutBeginning()
{
	// Andrew: this is called on the outside-most Containers.  as such, it
	//         is a good place to do smart things such as:
	// Andrew: BView: ConstrainClippingRegion(Region(-1,-1,-1,-1)) (!)
	//         which might prevent all drawing inside this view.
	//         BView: SetFlags(Flags() & ~B_WILL_DRAW) would stop Draw cold.
}


void
ContainerAdapter::layoutEnded()
{
	// Andrew: this is called on the outside-most Containers.  as such, it
	//         is a good place to do smart things such as:
	// Andrew: BView could Flush(), Sync() but they do all BViews 
	//         in the BWindow
	// Andrew: BView: ConstrainClippingRegion(NULL)
	//         to restore all drawing inside this view.
	//         BView: SetFlags(Flags() | B_WILL_DRAW) to restore Draw.
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Container.h"

jmethodID ContainerAdapter::findComponentAt_ID = NULL;

/*
 * Class:     java_awt_Container
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Container_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	ContainerAdapter::findComponentAt_ID = jenv->GetMethodID(jklass, "findComponentAt", "(IIZ)Ljava/awt/Component;");
	DASSERT(ContainerAdapter::findComponentAt_ID != NULL);
}

