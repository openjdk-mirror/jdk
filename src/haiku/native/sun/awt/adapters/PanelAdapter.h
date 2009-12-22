#ifndef PANEL_ADAPTER_H
#define PANEL_ADAPTER_H

#include "ContainerAdapter.h"

class Panel;

class PanelAdapter : public ContainerAdapter {
private:
	static Panel * NewPanel(JNIEnv * jenv, jobject jpeer, jobject jparent);
	Panel * panel;

public: 	// initialization
	        PanelAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent);

public: 	// destruction
	virtual ~PanelAdapter();
	
public: 	// JNI entry points
	virtual void	enable();
	virtual void	disable();

};

#endif // PANEL_ADAPTER_H
