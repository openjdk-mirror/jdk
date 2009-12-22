#ifndef SCROLL_PANE_ADAPTER_H
#define SCROLL_PANE_ADAPTER_H

#include "ContainerAdapter.h"
#include <interface/InterfaceDefs.h> // for orientation

class ScrollPane;

class ScrollPaneAdapter : public ContainerAdapter {
private:
	ScrollPane * scrollPane;

private:	// initialization
	static ScrollPane * NewScrollPane(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        ScrollPaneAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~ScrollPaneAdapter();
	
public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        int 	getHScrollbarHeight();
	        int 	getVScrollbarWidth();
	        void	setScrollPosition(int x, int y);
	        void	childResized(int w, int h);
	        void	setIncrements(orientation adj, int u, int b);
	        void	setValue(orientation adj, int v);

public: 	// heirarchy maintenance	
	virtual void	AttachChild(BView *view);
	virtual void	DetachChild(BView *view);
	
public: 	// -- Field and Method ID cache
	static jfieldID scrollbarDisplayPolicy_ID;
};

#endif // SCROLL_PANE_ADAPTER_H
