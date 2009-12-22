#include "PanelAdapter.h"
#include "Panel.h"
#include <cstdio>

/* static */ Panel * 
PanelAdapter::NewPanel(JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BRect frame = GetFrame(jenv, jpeer);
	Panel * value = new Panel(frame);
	return value;
}


PanelAdapter::PanelAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent) 
	: ContainerAdapter(jenv, jpeer, jparent, NewPanel(jenv, jpeer, jparent)),
	  panel(NULL)
{
	panel = dynamic_cast<Panel*>(GetView());
	DASSERT(panel != NULL);
	panel->SetAdapter(this);
}


PanelAdapter::~PanelAdapter()
{
}


// #pragma mark -
//
// JNI entry points
//

void
PanelAdapter::enable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}


void
PanelAdapter::disable()
{
	PRINT_PRETTY_FUNCTION();
	// TODO: anything?
}
