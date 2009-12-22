#ifndef WINDOW_ADAPTER_H
#define WINDOW_ADAPTER_H

#include "ContainerAdapter.h"

class BWindow;

class WindowAdapter : public ContainerAdapter {
private:
	BWindow *	window;
	BRect     	insets;
    jint     	state;
	bool     	iconified;

public:		// accessor
	BWindow * GetWindow() { return window; }

protected:	// for use by FileDialogAdapter only
	void	WindowDeleted() { window = NULL; }

public: 	// initialization
	static BWindow * NewWindow(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        WindowAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BWindow * window);

public: 	// destruction
	virtual ~WindowAdapter();

public: 	// JNI entry points
	virtual void	hide();
	virtual void	show();
	virtual void	enable();
	virtual void	disable();
	virtual void	updateWindow(); // Flush any paint events.
	virtual void	dispose();
	virtual BPoint  getLocationOnScreen();
	virtual void	nativeHandleEvent(jobject jawtEvent);
	virtual void	layoutBeginning();
	virtual void	layoutEnded();
	virtual bool	isPaintPending();
	        void    toFront();
	        void	toBack();
	        void 	setResizable(bool resizable);
	        void 	setTitle(const char * title);
	        void	setUndecorated(bool undecorated);
	static  float	getMinimumWidth();
	static  float	getMinimumHeight();
	        void	reshapeFrame(int x, int y, int width, int height);
	        int 	getScreenImOn();

protected:
	        void	SendWindowEvent(jint eventID);
public: 	// Haiku entry points
	virtual	void	FrameMoved(BPoint point);
	virtual	void	FrameResized(float width, float height);
	virtual	void	WindowActivated(bool state);
	        // BWindow
	virtual void	Minimize(bool minimize);
	virtual void	Zoom(BPoint position, float width, float height);
	virtual void	QuitRequested();

public: 	// heirarchy maintenance
	virtual void	AttachChild(BView *view);
	virtual void	DetachChild(BView *view);
	        void	UpdateDragger();
	
public: 	// -- surface functions
	virtual void	DrawSurface(BRect rect);
	virtual bool	ShouldClearSurface() { return false; }

public: 	// -- insets maintenance
	virtual BRect	GetInsets() { return insets; }
	virtual void	UpdateInsets();

public: 	// -- state (public for Window)
	        jint	GetState();
	        void	SetState(jint state);

};

#endif // WINDOW_ADAPTER_H
