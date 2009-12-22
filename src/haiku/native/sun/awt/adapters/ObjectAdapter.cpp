#include "ObjectAdapter.h"

#ifdef DEBUG
static bool reportEvents = FALSE;
#endif

/**
 * ObjectAdapter methods
 */
ObjectAdapter::ObjectAdapter()
	: BHandler("ObjectAdapter"),
	  jpeer(NULL),
	  callbacksEnabled(true)
{
}

ObjectAdapter::~ObjectAdapter()
{
}

/*
 * Link the C++ and Java peers.
 */
void 
ObjectAdapter::LinkObjects(JNIEnv * jenv, jobject jpeer)
{
	if (this->jpeer == NULL) {
		this->jpeer = jenv->NewGlobalRef(jpeer);
	}
	jenv->SetLongField(jpeer, nativeObject_ID, (jlong)this);
}


/**
 * Cleanup the C++ and Java linkings.
 */
void
ObjectAdapter::UnlinkObjects()
{
	if (jpeer != NULL) {
		JNIEnv * jenv = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
		jenv->SetLongField(jpeer, nativeObject_ID, (jlong)NULL);
		jenv->DeleteGlobalRef(jpeer);
		jpeer = NULL;
	}
}


/**
 * Return the peer associated with some target. This information is
 * maintained in a hashtable at the java level.
 */
jobject ObjectAdapter::GetPeerForTarget(JNIEnv * jenv, jobject jtarget)
{
	jobject jpeer = GetPeer(jenv);
	jobject jresult = jenv->CallObjectMethod(jpeer, getPeerForTarget_ID, jtarget);
	return jresult;
}

#ifdef DEBUG
#define WITH_PLATFORM_STRING(env, strexp, var)                                \
    if (1) {                                                                  \
        const char *var;                                                      \
        jstring _##var##str = (strexp);					      \
        if (_##var##str == NULL) {					      \
            JNU_ThrowNullPointerException((env), NULL);			      \
            goto _##var##end;                                                 \
        }                                                                     \
        var = JNU_GetStringPlatformChars((env), _##var##str, NULL);	      \
        if (var == NULL) goto _##var##end;

#define END_PLATFORM_STRING(env, var)                                         \
        JNU_ReleaseStringPlatformChars(env, _##var##str, var);                \
    _##var##end: ;                                                            \
    } else ((void)NULL)
#endif

/* Execute a callback to the associated Java peer. */
void
ObjectAdapter::DoCallback(const char* methodName, const char* methodSig, ...)
{
	JNIEnv * jenv = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	
	/* don't callback during the create & initialization process */
	if (jpeer != NULL && callbacksEnabled) {
		va_list args;
		va_start(args, methodSig);
#ifdef DEBUG
		if (reportEvents) {
			jstring targetStr = 
					(jstring)JNU_CallMethodByName(jenv, NULL, GetTarget(jenv), 
							"getName",
							"()Ljava/lang/String;").l;
			char *target;
			WITH_PLATFORM_STRING(jenv, targetStr, target) {
			printf("Posting %s%s method to %S\n", methodName, methodSig, 
					target);
			} END_PLATFORM_STRING(jenv, target);
		}
#endif
		/* caching would do much good here */
		JNU_CallMethodByNameV(jenv, NULL, GetPeer(jenv),
		                      methodName, methodSig, args);
		va_end(args);
	}
}


void ObjectAdapter::SendEvent(jobject jevent)
{
	JNIEnv * jenv = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

#ifdef DEBUG
	if (reportEvents) {
		jstring eventStr = JNU_ToString(jenv, jevent);
		jstring targetStr = 
				(jstring)JNU_CallMethodByName(jenv, NULL, GetTarget(jenv), "getName",
											"()LJava/lang/String;").l;
		
		printf("Posting %S to %S\n", eventStr, targetStr);
	}
#endif
	/* Post event to the system EventQueue */
	JNU_CallMethodByName(jenv, NULL, GetPeer(jenv), "postEvent",
			"(Ljava/awt/AWTEvent;)V", jevent);
}
