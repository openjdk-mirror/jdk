#include "CanvasAdapter.h"
#include "Canvas.h"

/* static */ Canvas *
CanvasAdapter::NewCanvas(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	Canvas * value = new Canvas(frame);
	return value;
}


CanvasAdapter::CanvasAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent)
	: ComponentAdapter(jenv, jpeer, jparent, NewCanvas(jenv, jpeer, jparent)),
	  canvas(NULL)
{
	canvas = dynamic_cast<Canvas*>(GetView());
	DASSERT(canvas != NULL);
	canvas->SetAdapter(this);
}


CanvasAdapter::~CanvasAdapter()
{
	// TODO: determine when (if) Canvas should be deleted
}


// #pragma mark -
//
// JNI entry points
//

void
CanvasAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	DEBUGGER();
}


void
CanvasAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	DEBUGGER();
}


void
CanvasAdapter::resetTargetGC()
{
	// TODO: reset target graphics context
	PRINT_PRETTY_FUNCTION();
	DEBUGGER();
}

