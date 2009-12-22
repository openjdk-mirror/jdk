#ifndef CANVAS_ADAPTER_H
#define CANVAS_ADAPTER_H

#include "ComponentAdapter.h"

class Canvas;

class CanvasAdapter : public ComponentAdapter {
private:
	Canvas * canvas;

private:	// initialization
	static Canvas * NewCanvas(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// initialization
	        CanvasAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~CanvasAdapter();

public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();
	        void	resetTargetGC();
};

#endif // CANVAS_ADAPTER_H
