#ifndef FRAME_ADAPTER_H
#define FRAME_ADAPTER_H

#include "WindowAdapter.h"

class FrameAdapter : public WindowAdapter {
public: 	// initialization
	static BWindow * NewFrame(JNIEnv * jenv, jobject jpeer, jobject jparent);
	        FrameAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BWindow * window);

public: 	// destruction
	virtual ~FrameAdapter();
	
public: 	// JNI entry points
	virtual void	dispose();
	        void	setMenuBar(BMenuBar * menubar);
	        void	setState(int state);
	        int 	getState();
	        void	setMaximizedBounds(int x, int y, int w, int h);
	        void	clearMaximizedBounds();

};

#endif // FRAME_ADAPTER_H
