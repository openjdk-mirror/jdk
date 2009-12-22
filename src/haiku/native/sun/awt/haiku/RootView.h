#ifndef ROOT_VIEW_H
#define ROOT_VIEW_H

#include <View.h>

class ComponentAdapter;

class RootView : public BView {
private:
	ComponentAdapter *	adapter;

public: 	// initialization & destruction
	        RootView(BRect bounds);
	virtual ~RootView();

public: 	// attach adapter
	        void	SetAdapter(ComponentAdapter * adapter);

public: 	// Haiku entry points
	virtual void	Draw(BRect rect);
	        // BView
	virtual void	MessageReceived(BMessage * message);
	virtual	void	MouseDown(BPoint point);
	virtual	void	MouseUp(BPoint point);
	virtual	void	MouseMoved(BPoint point, uint32 code, const BMessage * message);
	virtual	void	KeyDown(const char * bytes, int32 numBytes);
	virtual	void	KeyUp(const char * bytes, int32 numBytes);
};

#endif // ROOT_VIEW_H
