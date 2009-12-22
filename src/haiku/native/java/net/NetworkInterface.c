/*
 * Copyright 2000-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

#include <errno.h>
#include <strings.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <net/socket.h>
#include <net_settings.h> /* net_server specific */

#include "jvm.h"
#include "jni_util.h"
#include "net_util.h"
#include "if.h"

typedef struct _netaddr  {
    struct sockaddr *addr;
    int family; /* to make searches simple */
    struct _netaddr *next;
} netaddr;

typedef struct _netif {
    char *name;
    int index;
    netaddr *addr;
    struct _netif *next;
} netif;


// NS defines

#define DEVICECONFIG 	"DEVICECONFIG"
#define DEVICELINK		"DEVICELINK"
#define PRETTYNAME		"PRETTYNAME"
#define DEVICETYPE		"DEVICETYPE"
#define ETHERNET		"ETHERNET"
#define NETMASK			"NETMASK"
#define ENABLED			"ENABLED"
#define IPADDRESS   	"IPADDRESS"

#define INTERFACE		"interface%d"

#define NC_MAXVALLEN	256
_IMPEXP_NET char *_netconfig_find(const char *heading, const char *name, char *value,
					 int nbytes);
#define netconfig_find _netconfig_find


// BONE defines

#define BONE_SOCKIO_IOCTL_BASE 	0xbe230100
#define SIOCGIFADDR 						BONE_SOCKIO_IOCTL_BASE + 3
#define SIOCGIFFLAGS						BONE_SOCKIO_IOCTL_BASE + 7
#define SIOCGIFCOUNT 						BONE_SOCKIO_IOCTL_BASE + 10
#define SIOCGIFCONF 						BONE_SOCKIO_IOCTL_BASE + 11
#define SIOCGIFINDEX 						BONE_SOCKIO_IOCTL_BASE + 12
#define SIOCGIFNAME							BONE_SOCKIO_IOCTL_BASE + 13

#define IFF_UP          0x1   
#define IFF_LOOPBACK    0x8             /* is a loopback net */ 



/************************************************************************
 * NetworkInterface
 */

#include "java_net_NetworkInterface.h"

/************************************************************************
 * NetworkInterface
 */
jclass ni_class;
jfieldID ni_nameID;
jfieldID ni_indexID;
jfieldID ni_descID;
jfieldID ni_addrsID;
jmethodID ni_ctrID;

static jclass ni_iacls;
static jclass ni_ia4cls;
static jclass ni_ia6cls;
static jmethodID ni_ia4ctrID;
static jmethodID ni_ia6ctrID;
static jfieldID ni_iaaddressID;
static jfieldID ni_iafamilyID;
static jfieldID ni_ia6ipaddressID;

static jobject createNetworkInterface(JNIEnv *env, netif *ifs);

static netif *enumInterfaces(JNIEnv *env);
static netif *enumInterfaces_NetServer(JNIEnv *env, netif *ifs);
static netif *enumInterfaces_BONE(JNIEnv *env, netif *ifs);

static netif *addif(JNIEnv *env, netif *ifs, char *if_name, int index, 
		    int family, struct sockaddr *new_addrP, int new_addrlen);
static void freeif(netif *ifs);


/*
 * Class:     java_net_NetworkInterface
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_net_NetworkInterface_init(JNIEnv *env, jclass cls) {
    ni_class = (*env)->FindClass(env,"java/net/NetworkInterface");
    ni_class = (*env)->NewGlobalRef(env, ni_class);
    ni_nameID = (*env)->GetFieldID(env, ni_class,"name", "Ljava/lang/String;");
    ni_indexID = (*env)->GetFieldID(env, ni_class, "index", "I");
    ni_addrsID = (*env)->GetFieldID(env, ni_class, "addrs", "[Ljava/net/InetAddress;");
    ni_descID = (*env)->GetFieldID(env, ni_class, "displayName", "Ljava/lang/String;");
    ni_ctrID = (*env)->GetMethodID(env, ni_class, "<init>", "()V");

    ni_iacls = (*env)->FindClass(env, "java/net/InetAddress");
    ni_iacls = (*env)->NewGlobalRef(env, ni_iacls);
    ni_ia4cls = (*env)->FindClass(env, "java/net/Inet4Address");
    ni_ia4cls = (*env)->NewGlobalRef(env, ni_ia4cls);
    ni_ia6cls = (*env)->FindClass(env, "java/net/Inet6Address");
    ni_ia6cls = (*env)->NewGlobalRef(env, ni_ia6cls);
    ni_ia4ctrID = (*env)->GetMethodID(env, ni_ia4cls, "<init>", "()V");
    ni_ia6ctrID = (*env)->GetMethodID(env, ni_ia6cls, "<init>", "()V");
    ni_iaaddressID = (*env)->GetFieldID(env, ni_iacls, "address", "I");
    ni_iafamilyID = (*env)->GetFieldID(env, ni_iacls, "family", "I");
    ni_ia6ipaddressID = (*env)->GetFieldID(env, ni_ia6cls, "ipaddress", "[B");
}


/*
 * Class:     java_net_NetworkInterface
 * Method:    getByName0
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByName0
    (JNIEnv *env, jclass cls, jstring name) {

    netif *ifs, *curr;
    jboolean isCopy;
    const char* name_utf = (*env)->GetStringUTFChars(env, name, &isCopy);
    jobject obj = NULL;

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
	return NULL;
    }

    /*
     * Search the list of interface based on name
     */
    curr = ifs;
    while (curr != NULL) {
        if (strcmp(name_utf, curr->name) == 0) {
            break;
        }
        curr = curr->next;
    }

    /* if found create a NetworkInterface */
    if (curr != NULL) {;
        obj = createNetworkInterface(env, curr);
    }

    /* release the UTF string and interface list */
    (*env)->ReleaseStringUTFChars(env, name, name_utf);
    freeif(ifs);

    return obj;
}


/*
 * Class:     java_net_NetworkInterface
 * Method:    getByIndex
 * Signature: (Ljava/lang/String;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByIndex
    (JNIEnv *env, jclass cls, jint index) {

    netif *ifs, *curr;
    jobject obj = NULL;

    if (index <= 0) {
        return NULL;
    }

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
        return NULL;
    }

    /*
     * Search the list of interface based on index
     */
    curr = ifs;
    while (curr != NULL) {
	if (index == curr->index) {
            break;
        }
        curr = curr->next;
    }

    /* if found create a NetworkInterface */
    if (curr != NULL) {;
        obj = createNetworkInterface(env, curr);
    }

    freeif(ifs);
    return obj;
}

/*
 * Class:     java_net_NetworkInterface
 * Method:    getByInetAddress0
 * Signature: (Ljava/net/InetAddress;)Ljava/net/NetworkInterface;
 */
JNIEXPORT jobject JNICALL Java_java_net_NetworkInterface_getByInetAddress0
    (JNIEnv *env, jclass cls, jobject iaObj) {

    netif *ifs, *curr;
    int family = AF_INET;
    jobject obj = NULL;
    jboolean match = JNI_FALSE;

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
	return NULL;
    }

    curr = ifs;
    while (curr != NULL) {
	netaddr *addrP = curr->addr;

	/*
	 * Iterate through each address on the interface
	 */
	while (addrP != NULL) {

	    if (family == addrP->family) {
		if (family == AF_INET) {
		    int address1 = htonl(((struct sockaddr_in*)addrP->addr)->sin_addr.s_addr);
                    int address2 = (*env)->GetIntField(env, iaObj, ni_iaaddressID);

                    if (address1 == address2) {
			match = JNI_TRUE;
			break;
		    }
		}

	    }

	    if (match) {
		break;
	    }
	    addrP = addrP->next;
	}

	if (match) {
	    break;
	}
	curr = curr->next;
    }

    /* if found create a NetworkInterface */
    if (match) {;
        obj = createNetworkInterface(env, curr);
    }

    freeif(ifs);
    return obj;
}


/*
 * Class:     java_net_NetworkInterface
 * Method:    getAll
 * Signature: ()[Ljava/net/NetworkInterface;
 */
JNIEXPORT jobjectArray JNICALL Java_java_net_NetworkInterface_getAll
    (JNIEnv *env, jclass cls) {

    netif *ifs, *curr;
    jobjectArray netIFArr;
    jint arr_index, ifCount;

    ifs = enumInterfaces(env);
    if (ifs == NULL) {
        return NULL;
    }

    /* count the interface */
    ifCount = 0;
    curr = ifs;
    while (curr != NULL) { 
	ifCount++;
	curr = curr->next;
    } 

    /* allocate a NetworkInterface array */
    netIFArr = (*env)->NewObjectArray(env, ifCount, cls, NULL);
    if (netIFArr == NULL) {
	freeif(ifs);
        return NULL;
    }

    /*
     * Iterate through the interfaces, create a NetworkInterface instance
     * for each array element and populate the object.
     */
    curr = ifs;
    arr_index = 0;
    while (curr != NULL) {
        jobject netifObj;

        netifObj = createNetworkInterface(env, curr);
        if (netifObj == NULL) {
	    freeif(ifs);
            return NULL;
        }

        /* put the NetworkInterface into the array */
        (*env)->SetObjectArrayElement(env, netIFArr, arr_index++, netifObj);

        curr = curr->next;
    }

    free(ifs);
    return netIFArr;
}

/*
 * Create a NetworkInterface object, populate the name and index, and
 * populate the InetAddress array based on the IP addresses for this
 * interface.
 */
jobject createNetworkInterface(JNIEnv *env, netif *ifs)
{
    jobject netifObj;
    jobject name;
    jobjectArray addrArr;
    netaddr *addrs;
    jint addr_index, addr_count;
    netaddr *addrP;

    /*
     * Create a NetworkInterface object and populate it
     */
    netifObj = (*env)->NewObject(env, ni_class, ni_ctrID); 
    name = (*env)->NewStringUTF(env, ifs->name);
    if (netifObj == NULL || name == NULL) {
        return NULL;
    }
    (*env)->SetObjectField(env, netifObj, ni_nameID, name);
    (*env)->SetObjectField(env, netifObj, ni_descID, name); 
    (*env)->SetIntField(env, netifObj, ni_indexID, ifs->index);

    /*
     * Count the number of address on this interface
     */
    addr_count = 0;
    addrP = ifs->addr;
    while (addrP != NULL) {
	addr_count++;
  	addrP = addrP->next;
    }

    /*
     * Create the array of InetAddresses
     */
    addrArr = (*env)->NewObjectArray(env, addr_count,  ni_iacls, NULL);
    if (addrArr == NULL) {
        return NULL;
    }

    addrP = ifs->addr;
    addr_index = 0;
    while (addrP != NULL) {
	jobject iaObj = NULL;

	if (addrP->family == AF_INET) {
            iaObj = (*env)->NewObject(env, ni_ia4cls, ni_ia4ctrID);
            if (iaObj) {
                 (*env)->SetIntField(env, iaObj, ni_iaaddressID, 
                     htonl(((struct sockaddr_in*)addrP->addr)->sin_addr.s_addr));
	    }
	}

#ifdef AF_INET6
	if (addrP->family == AF_INET6) {
	    iaObj = (*env)->NewObject(env, ni_ia6cls, ni_ia6ctrID);	
	    if (iaObj) {
		jbyteArray ipaddress = (*env)->NewByteArray(env, 16);
		if (ipaddress == NULL) {
		    return NULL;
		}
		(*env)->SetByteArrayRegion(env, ipaddress, 0, 16,
                    (jbyte *)&(((struct sockaddr_in6*)addrP->addr)->sin6_addr));
		(*env)->SetObjectField(env, iaObj, ni_ia6ipaddressID, ipaddress);
	    } 
	}
#endif

	if (iaObj == NULL) {
	    return NULL;
	}

        (*env)->SetObjectArrayElement(env, addrArr, addr_index++, iaObj);
        addrP = addrP->next;
    }
 
    (*env)->SetObjectField(env, netifObj, ni_addrsID, addrArr);

    /* return the NetworkInterface */
    return netifObj;
}

/* 
 * Enumerates all interfaces
 */
static netif *enumInterfaces(JNIEnv *env) {
    netif *ifs;

    /*
     * Enumerate interfaces via net_server API
     */
    ifs = enumInterfaces_NetServer(env, NULL);

	/*
	 * No interfaces returned? Are we using BONE?
	 */
	if (ifs == NULL)
		ifs = enumInterfaces_BONE(env, NULL);	
    
    /*
     * No interfaces in the system. Shame.
     */
    if (ifs == NULL) {
		if ((*env)->ExceptionOccurred(env))
	    	return NULL;
    }

    return ifs;
}

/*
 * Enumerates and returns all IPv4 interfaces via net_server
 */
static netif *enumInterfaces_NetServer(JNIEnv *env, netif *ifs) {
	char iface[14];
	char name[NC_MAXVALLEN];
	char value[NC_MAXVALLEN];
	struct sockaddr sa;
	int i;

	memset(&sa, 0, sizeof(sa));

	for(i=0; i<256; i++) {

		/* Prepare a name to lookup: interfaceX */
		sprintf(iface, INTERFACE, i);

		/* Get device link, if fails  */
		if (!netconfig_find(iface, DEVICELINK, name, NC_MAXVALLEN))
			return ifs;

		netconfig_find(iface, ENABLED, value, NC_MAXVALLEN);
		if (value[0] == '0')
			continue; /* Interface is down -- skip it */

		sa.sa_family = AF_INET;
		if (!netconfig_find(iface, IPADDRESS, sa.sa_data, NC_MAXVALLEN)) {
			continue; /* Can't get ip address - weird */
		}
		/*
		 * Add to the list 
		 */
		ifs = addif(env, ifs, name, -1, AF_INET,
		    (struct sockaddr *)&sa,
		    sizeof(struct sockaddr_in));
	}

	return ifs;
}


/*
 * Enumerates and returns all IPv4 interfaces via BINE
 */
static netif *enumInterfaces_BONE(JNIEnv *env, netif *ifs) {
	struct sockaddr_in *skt;
	int sock;
	long numifs = 0;
	struct ifconf ifc;	
	struct ifreq ifreq, *ifr, *ifend, ifbuf[16];

	if ((sock = JVM_Socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
		/*
		 * If EPROTONOSUPPORT is returned it means we don't have
		 * IPv4 support so don't throw an exception.
		 */
		if (errno != EPROTONOSUPPORT) {
		    NET_ThrowByNameWithLastError(env , JNU_JAVANETPKG "SocketException",
	                            "Socket creation failed");
		}
		return ifs;
	}
	
	ifc.ifc_len = sizeof(ifs);
	ifc.ifc_req = ifbuf;
	
	if (ioctl(sock, SIOCGIFCONF, &ifc) < 0) {
        NET_ThrowByNameWithLastError(env , JNU_JAVANETPKG "SocketException",
                         "ioctl SIOCGIFCONF failed");
        closesocket(sock);
        return ifs;
	}
	
	ifend = ifbuf + (ifc.ifc_len / sizeof (struct ifreq));
	for (ifr = ifc.ifc_req; ifr < ifend; ifr++) {
	 	strncpy(ifreq.ifr_name, ifr->ifr_name, 
			sizeof(ifreq.ifr_name));
		ifreq.ifr_index = ifr->ifr_index;

		/*
		 * Add to the list 
		 */
		ifs = addif(env, ifs, ifr->ifr_name, ifr->ifr_index, AF_INET,
		    (struct sockaddr *)&(ifr->ifr_addr),
		    sizeof(struct sockaddr_in));

		/*
		 * If an exception occurred then free the list
		 */
		if ((*env)->ExceptionOccurred(env)) {
		    closesocket(sock);
		    freeif(ifs);
		    return NULL;
		}
	}
	closesocket(sock);
	return ifs;
}

/*
 * Free an interface list (including any attached addresses)
 */
void freeif(netif *ifs) {
    netif *currif = ifs;

    while (currif != NULL) {
	netaddr *addrP = currif->addr;
	while (addrP != NULL) {
	    netaddr *next = addrP->next;
	    free(addrP->addr); 
	    free(addrP);
	    addrP = next;
	}

	free(currif->name);

	ifs = currif->next;
	free(currif);
	currif = ifs;
    }
}

/*
 * Add an interface to the list. If known interface just link
 * a new netaddr onto the list. If new interface create new
 * netif structure.
 */
netif *addif(JNIEnv *env, netif *ifs, char *if_name, int index, int family,
	     struct sockaddr *new_addrP, int new_addrlen) {
    netif *currif = ifs;
    netaddr *addrP;
    char name[IFNAMSIZ];
    char *unit;

    /*
     * If the interface name is a logical interface then we
     * remove the unit number so that we have the physical
     * interface (eg: hme0:1 -> hme0). NetworkInterface
     * currently doesn't have any concept of physical vs.
     * logical interfaces.
     */
    strcpy(name, if_name);
    unit = strchr(name, ':');
    if (unit != NULL) {
	*unit = '\0';
    }

    /*
     * Create and populate the netaddr node. If allocation fails
     * return an un-updated list.
     */
    addrP = (netaddr *)malloc(sizeof(netaddr));
    if (addrP) {
	addrP->addr = (struct sockaddr *)malloc(new_addrlen);
	if (addrP->addr == NULL) {
	    free(addrP);
	    addrP = NULL;
	}
    }
    if (addrP == NULL) {
	JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
	return ifs; /* return untouched list */
    }
    memcpy(addrP->addr, new_addrP, new_addrlen);
    addrP->family = family;

    /*
     * Check if this is a "new" interface. Use the interface
     * name for matching because index isn't supported in
     * net_server.
     */
    while (currif != NULL) {
        if (strcmp(name, currif->name) == 0) {
	    break;
        }
	currif = currif->next;
    }

    /*
     * If "new" then create an netif structure and
     * insert it onto the list.
     */
    if (currif == NULL) {
	currif = (netif *)malloc(sizeof(netif));
	if (currif) {
	    currif->name = strdup(name);
	    if (currif->name == NULL) {
		free(currif);
		currif = NULL;
	    }
	}
	if (currif == NULL) {
	    JNU_ThrowOutOfMemoryError(env, "heap allocation failed");
	    return ifs;
 	}
	currif->index = index;
	currif->addr = NULL;
	currif->next = ifs;
	ifs = currif;
    }

    /*
     * Finally insert the address on the interface
     */
    addrP->next = currif->addr;
    currif->addr = addrP;

    return ifs;
}
