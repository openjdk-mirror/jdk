#ifndef CONTAINER_ADAPTER_H
#define CONTAINER_ADAPTER_H

#include "ComponentAdapter.h"

class ContainerAdapter : public ComponentAdapter {
private:
	BView * view;

protected: 	// initialization
	ContainerAdapter(JNIEnv * jenv, jobject jpeer, jobject jparent, BView * view);

public: 	// destruction
	virtual ~ContainerAdapter();
	
public: 	// JNI entry points
	virtual void	layoutBeginning();
	virtual void	layoutEnded();

public: 	// heirarchy maintenance	
	virtual void	AttachChild(BView *view);
	virtual void	DetachChild(BView *view);
	
public: 	// -- Field and Method ID cache
	static jfieldID	 insets_ID;
	static jmethodID findComponentAt_ID;
};

#endif // CONTAINER_ADAPTER_H
