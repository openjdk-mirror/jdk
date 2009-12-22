#ifndef WINDOW_H
#define WINDOW_H

#include <interface/Region.h>
#include <interface/Window.h>
#include <game/DirectWindow.h>
#include <MessageFilter.h>
#include <OS.h>

#define BDirectWindow BWindow

class WindowAdapter;
class EventEnvironment;

class Window : public BDirectWindow {
private:
	WindowAdapter * adapter;
	EventEnvironment * environment;

public: 	// accessors
	EventEnvironment * Environment() { return environment; }
	void SetAdapter(WindowAdapter * adapter);

public: 	// initialization & destruction
	        Window(BRect frame, const char * title,
	               window_look look, window_feel feel);
	virtual ~Window();

private:	// Utility functions
	        void	SetupEnvironment();
	        void	ReleaseEnvironment();

public: 	// Haiku entry points
	virtual void	DispatchMessage(BMessage * message, BHandler * handler);
	        // BWindow
	virtual void	MessageReceived(BMessage * message);
	virtual	void	FrameMoved(BPoint point);
	virtual	void	FrameResized(float width, float height);
	virtual void	Minimize(bool minimize);
	virtual	void	WindowActivated(bool state);
	virtual void	Zoom(BPoint position, float width, float height);
	virtual	void	MenusBeginning();
	virtual	void	MenusEnded();
	virtual void	WorkspaceActivated(int32 workspaces, bool state);
	virtual void	WorkspacesChanged(uint32 old_workspaces, uint32 new_workspaces);
	virtual void	ScreenChanged(BRect screen_size, color_space depth);
	        // BDirectWindow
	virtual void	DirectConnected(direct_buffer_info *info);

private:
	// BDirectWindow size tracking.
	bool        	fConnected;
	BRegion     	fPrevClip;
};

#endif // WINDOW_H
