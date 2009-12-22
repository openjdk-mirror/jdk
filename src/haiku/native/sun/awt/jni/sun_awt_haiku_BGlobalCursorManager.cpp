#include "sun_awt_haiku_BGlobalCursorManager.h"
#include "GlobalCursorAdapter.h"
#include "ContainerAdapter.h"
#include "AwtPoint.h"
#include "Cursor.h"

/*
 * Class:     sun_awt_haiku_BGlobalCursorManager
 * Method:    _setCursor
 * Signature: (Ljava/awt/Cursor;Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BGlobalCursorManager__1setCursor
  (JNIEnv * jenv, jobject jpeer, jobject jcursor, jboolean useCache)
{
	const BCursor * cursor = Cursor::GetCursor(jenv, jcursor);
	GlobalCursorAdapter::GetInstance()->setCursor(cursor, useCache);
}

/*
 * Class:     sun_awt_haiku_BGlobalCursorManager
 * Method:    _getCursorPosition
 * Signature: (Ljava/awt/Point;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BGlobalCursorManager__1getCursorPosition
  (JNIEnv * jenv, jobject jpeer, jobject point)
{
	BPoint location = GlobalCursorAdapter::GetInstance()->getCursorPosition();
	
	AwtPoint::SetLocation(jenv, point, location.x, location.y);
	
	return;
}

/*
 * Class:     sun_awt_haiku_BGlobalCursorManager
 * Method:    _findComponentAt
 * Signature: (Ljava/awt/Container;II)Ljava/awt/Component;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BGlobalCursorManager__1findComponentAt
  (JNIEnv * jenv, jobject jpeer, jobject jcontainer, jint x, jint y)
{
    /*
     * Call private version of Container.findComponentAt with the following
     * flag set: ignoreEnabled = false (i.e., don't return or recurse into
     * disabled Components).  
     * NOTE: it may return a JRootPane's glass pane as the target Component.
     */
	jobject component
		 = jenv->CallObjectMethod(jcontainer, ContainerAdapter::findComponentAt_ID, 
		                                      x, y, false);
	return component;
}


/*
 * Class:     sun_awt_haiku_BGlobalCursorManager
 * Method:    _getComponentUnderCursor
 * Signature: ()Ljava/awt/Component;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BGlobalCursorManager__1getComponentUnderCursor
  (JNIEnv * jenv, jobject jpeer)
{
	return GlobalCursorAdapter::GetInstance()->getComponentUnderCursor(jenv);
}
