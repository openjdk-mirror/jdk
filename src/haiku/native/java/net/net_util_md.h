/*
 * Copyright 1997-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

#ifndef NET_UTILS_MD_H
#define NET_UTILS_MD_H

#include <sys/socket.h>
#include <sys/types.h>
#include <netdb.h>
#include <unistd.h>

#ifndef USE_SELECT
#include <poll.h>
#endif

#define uint32_t uint32
#define uint8_t uint8

typedef unsigned short  sa_family_t;
typedef unsigned short  in_port_t;

#ifndef SO_LINGER

struct linger {
	int l_onoff;
	int l_linger;
};

#endif

struct ip_mreq {
    struct in_addr imr_multiaddr;  /* IP multicast address of group */
    struct in_addr imr_interface;  /* local IP address of interface */
};

#ifndef AF_INET6
#include "ipv6_defs.h"
#endif

#define NET_Timeout	JVM_Timeout
#define NET_Read	JVM_Recv
#define NET_RecvFrom	JVM_RecvFrom
#define NET_ReadV	readv
#define NET_Send	JVM_Send
#define NET_SendTo	JVM_SendTo
#define NET_WriteV	writev
#define NET_Connect	JVM_Connect
#define NET_Accept	JVM_Accept
#define NET_SocketClose JVM_SocketClose
#define NET_Dup2	dup2
#define NET_Select	select
#define NET_Poll	poll

/* needed from libsocket on Solaris 8 */

typedef int (*getaddrinfo_f)(const char *nodename, const char  *servname,
     const struct addrinfo *hints, struct addrinfo **res);

typedef void (*freeaddrinfo_f)(struct addrinfo *);

typedef int (*getnameinfo_f)(const struct sockaddr *, size_t,
    char *, size_t, char *, size_t, int);

extern getaddrinfo_f getaddrinfo_ptr;
extern freeaddrinfo_f freeaddrinfo_ptr;
extern getnameinfo_f getnameinfo_ptr;

/* do we have address translation support */

extern jboolean NET_addrtransAvailable();

/************************************************************************
 * Macros and constants
 */

#define MAX_BUFFER_LEN 2048
#define MAX_HEAP_BUFFER_LEN 65536

#define SOCKADDR    	union { struct sockaddr_in him4; }
#define SOCKADDR_LEN 	sizeof(SOCKADDR)

/************************************************************************
 *  Utilities
 */

void NET_ThrowByNameWithLastError(JNIEnv *env, const char *name,
                   const char *defaultDetail);

// Values taken from OBOS mean that getsockopt/setsockopt
// will start magically working when our binary is moved
// to OBOS. (without recompilation)  This is performed by
// checking for ENOPROTOOPT from getsockopt/getsockopt if
// they fail.  R5 returns this for every value except 5.
// If OBOS does not support an option, it will get the R5
// default simulation behavior, which should be correct.

#define TCP_NODELAY 1
#define SO_KEEPALIVE    0x0008          /* keep connections alive */
#define SO_BROADCAST    0x0020          /* permit sending of broadcast msgs */
#define SO_LINGER       0x0080          /* linger on close if data present */
#define SO_OOBINLINE    0x0100          /* leave received OOB data in line */
#define SO_SNDBUF    0x1001
#define SO_RCVBUF    0x1002
#define IPPROTO_IP 0 /* 0, IPv4 */
#define IP_TOS                   3   /* int; IP type of service and preced. */
#define IP_MULTICAST_IF          9   /* in_addr; set/get IP multicast i/f  */
#define IP_MULTICAST_TTL        10   /* u_char; set/get IP multicast ttl */
#define IP_MULTICAST_LOOP       11   /* u_char; set/get IP multicast loopback */ 
#define IP_ADD_MEMBERSHIP       12   /* ip_mreq; add an IP group membership */
#define IP_DROP_MEMBERSHIP      13   /* ip_mreq; drop an IP group membership */ 

#endif /* NET_UTILS_MD_H */
