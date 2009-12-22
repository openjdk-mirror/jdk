#include "PopupMenuAdapter.h"
#include <app/Message.h>
#include <interface/PopUpMenu.h>
#include <interface/Window.h>
#include <interface/MenuItem.h>
#include <cstdio>

/* static */ BPopUpMenu *
PopupMenuAdapter::NewPopupMenu(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	// TOTRY: pass target as an argument
	jobject jjavaObject = jenv->GetObjectField(jpeer, javaObject_ID);
	DASSERT(jjavaObject != NULL);
	// TOTRY: pass label as an argument
	BPopUpMenu * value = new BPopUpMenu("PopupMenu", false, false);
	return value;
}


PopupMenuAdapter::PopupMenuAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: MenuAdapter(jenv, jpeer, jparent, NewPopupMenu(jenv, jpeer, jparent)),
	  popup(NULL)
{
	popup = dynamic_cast<BPopUpMenu*>(Menu());
	DASSERT(popup != NULL);
}


PopupMenuAdapter::~PopupMenuAdapter()
{
	DASSERT(popup == NULL);
}


// #pragma mark -
//
// JNI entry points
//

/* virtual */ void
PopupMenuAdapter::dispose()
{
	// menu will be deleted when the containing BMenuItem is deleted in dispose
	popup = NULL;
	MenuAdapter::dispose();
}


void
PopupMenuAdapter::show(BView * view, int x, int y)
{
	BPoint point(x, y);
	if (view->LockLooper()) {
		view->ConvertToScreen(&point);
		view->UnlockLooper();
	} else {
		LOCK_LOOPER_FAILED();
	}
	BRect clickToOpen(point.x-1,point.y-1,point.x+1,point.y+1);
	// We bring up the menu even if we failed to convert the point,
	// since this still gets us partial behavior.  Things can still
	// be selected.
	BMenuItem * item = popup->Go(point, false, true, clickToOpen, false);
	if (item != NULL) {
		BWindow * window = view->Window();
		DASSERT(window != NULL);
		BMessage message(*item->Message());
		message.AddInt64("when", system_time());
		window->PostMessage(&message, window);
	}
}

