#ifndef COMPONENT_ADAPTER_H
#define COMPONENT_ADAPTER_H

#include "ObjectAdapter.h"
#include <GraphicsDefs.h>
#include <Point.h>
#include <Rect.h>

#define AWT_FOCUS_REQUESTED 'aFCR'

class EventEnvironment;
class BBitmap;
class BView;

class ComponentAdapter : public ObjectAdapter {
private:
	jobject   jparent;
	BBitmap * surface;
	BView   * view;

	// visibility
	bool      visible;

	// mouse state
	int32	  oldModifiers;
	int32	  oldButtons;
	int32	  oldClicks;
	int32	  oldX;
	int32	  oldY;
	BPoint    mouseScreenLocation;

public: 	// accessors
	BView   * GetView()    { return view; }
	BBitmap * GetSurface() { return surface; }
protected:	// for use by FileDialogAdapter only
	void	ComponentDeleted() { view = NULL; }

protected: 	// utility function
	static  BRect	GetFrame(JNIEnv * jenv, jobject jpeer);

protected:	// initialization
	        ComponentAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BView * view);

public: 	// destruction
	virtual ~ComponentAdapter();

public: 	// JNI entry points
	virtual void	hide();
	virtual void	show();
	virtual void	enable() = 0;
	virtual void	disable() = 0;
	virtual void	updateWindow(); // Flush any paint events.
	virtual void	dispose();
	virtual BPoint  getLocationOnScreen();
	virtual void	nativeHandleEvent(jobject jawtEvent);
	        void	reshape(int x, int y, int width, int height);
	        void	setForeground(rgb_color color);
	        void	setBackground(rgb_color color);
	        void	setFont(BFont font);
	        void	addDropTarget(jobject jdropTarget);
	        void	removeDropTarget(jobject jdropTarget);

protected:  // utility functions
	virtual EventEnvironment * Environment();
	BMessage *        	CurrentMessage();
	        void      	FireMouseEvent();
	        void      	FireKeyboardEvent();
	        void	    ModifiersChanged();
	        void	    MouseWheelChanged();
	        void     	MenuItemInvoked(BMessage * message);

public:     // Haiku entry points
	virtual void	MessageReceived(BMessage * message); // don't override
	        // BView
	virtual void	MakeFocus(bool state = true);
	virtual	void	MouseDown(BPoint point);
	virtual	void	MouseUp(BPoint point);
	virtual	void	MouseMoved(BPoint point, uint32 code, const BMessage * message);
	virtual	void	WindowActivated(bool state);
	virtual	void	KeyDown(const char * bytes, int32 numBytes);
	virtual	void	KeyUp(const char * bytes, int32 numBytes);

protected:	// BInvoker "entry points"
	virtual void	InvocationReceived(BMessage * message);
	virtual void	SelectionReceived(BMessage * message);

protected:	// BFilePanel "entry points"
	virtual void	RefsReceived(BMessage * message);
	virtual void	SaveReceived(BMessage * message);
	virtual void	CancelReceived(BMessage * message);

public: 	// -- surface functions
	virtual void	HandleExpose(JNIEnv * jenv, BRect * clip);
protected:
	virtual void    CreateSurface(BRect bounds, bool sendExpose);
	virtual void	RecreateSurface(bool sendExpose);
public:
	virtual void	RepaintView(BRect rect);
	virtual void	DrawSurface(BRect rect);
protected:
	virtual bool	ShouldClearSurface() { return true; }

public: 	// insets
	virtual BRect	GetInsets();

public: 	// -- Field and Method ID cache
	static jfieldID peer_ID;
	static jfieldID	privateKey_ID;
	static jfieldID	x_ID;
	static jfieldID	y_ID;
	static jfieldID	width_ID;
	static jfieldID	height_ID;
};

#endif // COMPONENT_ADAPTER_H
