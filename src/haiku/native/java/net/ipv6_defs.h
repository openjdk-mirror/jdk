/*
 * Copyright 2002-2003 Sun Microsystems, Inc.  All Rights Reserved.
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
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>

#ifndef IPV6_DEFS_H
#define IPV6_DEFS_H

/* from socket.h */
#define AF_INET6	26		/* Internet Protocol, Version 6 */
#define PF_INET6        AF_INET6
/* from in/netdb.h */
#define IPPROTO_IPV6	41              /* IPv6 encapsulated in IP */

/*
 * IPv6 options
 */
#define IPV6_UNICAST_HOPS	0x5	/* hop limit value for unicast */
					/* packets. */
					/* argument type: uint_t */
#define IPV6_MULTICAST_IF	0x6	/* outgoing interface for */
					/* multicast packets. */
					/* argument type: struct in6_addr */
#define IPV6_MULTICAST_HOPS	0x7	/* hop limit value to use for */
					/* multicast packets. */
					/* argument type: uint_t */
#define IPV6_MULTICAST_LOOP	0x8	/* enable/disable delivery of */
					/* multicast packets on same socket. */
					/* argument type: uint_t */
#define IPV6_JOIN_GROUP		0x9	/* join an IPv6 multicast group. */
					/* argument type: struct ipv6_mreq */
#define IPV6_LEAVE_GROUP	0xa	/* leave an IPv6 multicast group */
					/* argument type: struct ipv6_mreq */
/*
 * IPV6_ADD_MEMBERSHIP and IPV6_DROP_MEMBERSHIP are being kept
 * for backward compatibility. They have the same meaning as IPV6_JOIN_GROUP
 * and IPV6_LEAVE_GROUP respectively.
 */
#define IPV6_ADD_MEMBERSHIP	0x9	 /* join an IPv6 multicast group. */
					/* argument type: struct ipv6_mreq */
#define IPV6_DROP_MEMBERSHIP	0xa	/* leave an IPv6 multicast group */
					/* argument type: struct ipv6_mreq */


/* addrinfo flags */
#define AI_PASSIVE      0x0008  /* intended for bind() + listen() */
#define AI_CANONNAME    0x0010  /* return canonical version of host */
#define AI_NUMERICHOST  0x0020  /* use numeric node address string */

/* getnameinfo max sizes as defined in spec */

#define NI_MAXHOST      1025
#define NI_MAXSERV      32

/* getnameinfo flags */

#define NI_NOFQDN       0x0001
#define NI_NUMERICHOST  0x0002  /* return numeric form of address */
#define NI_NAMEREQD     0x0004  /* request DNS name */
#define NI_NUMERICSERV  0x0008
#define NI_DGRAM        0x0010



struct in6_addr {
  union {
    /*
     * Note: Static initalizers of "union" type assume
     * the constant on the RHS is the type of the first member
     * of union.
     * To make static initializers (and efficient usage) work,
     * the order of members exposed to user and kernel view of
     * this data structure is different.
     * User environment sees specified uint8_t type as first
     * member whereas kernel sees most efficient type as
     * first member.
     */
#ifdef _KERNEL
    uint32_t        _S6_u32[4];     /* IPv6 address */
    uint8_t         _S6_u8[16];     /* IPv6 address */
#else
    uint8_t         _S6_u8[16];     /* IPv6 address */
    uint32_t        _S6_u32[4];     /* IPv6 address */
#endif
    uint32_t        __S6_align;     /* Align on 32 bit boundary */
  } _S6_un;
};
#define s6_addr         _S6_un._S6_u8

/*
 * IPv6 socket address.
 */
struct sockaddr_in6 {
  sa_family_t     sin6_family;
  in_port_t       sin6_port;
  uint32_t        sin6_flowinfo;
  struct in6_addr sin6_addr;
  uint32_t        sin6_scope_id;  /* Depends on scope of sin6_addr */
  uint32_t        __sin6_src_id;  /* Impl. specific - UDP replies */
};


/*
 * Argument structure for IPV6_JOIN_GROUP and IPV6_LEAVE_GROUP on
 * IPv6 addresses.
 */
struct ipv6_mreq {
  struct in6_addr	ipv6mr_multiaddr;       /* IPv6 multicast addr */
  unsigned int		ipv6mr_interface;       /* interface index */
};


struct addrinfo {
  int ai_flags;		/* AI_PASSIVE, AI_CANONNAME */
  int ai_family;		/* PF_xxx */
  int ai_socktype;	/* SOCK_xxx */
  int ai_protocol;	/* 0 or IPPROTO_xxx for IPv4 and IPv6 */
  size_t ai_addrlen;	/* length of ai_addr */
  char *ai_canonname;	/* canonical name for hostname */
  struct sockaddr *ai_addr;	/* binary address */
  struct addrinfo *ai_next;	/* next structure in linked list */
};

/*
 * sockaddr_storage:
 * Common superset of at least AF_INET, AF_INET6 and AF_LINK sockaddr
 * structures. Has sufficient size and alignment for those sockaddrs.
 */

/*
 * Desired maximum size, alignment size and related types.
 */
#define _SS_MAXSIZE     256     /* Implementation specific max size */

/*
 * To represent desired sockaddr max alignment for platform, a
 * type is chosen which may depend on implementation platform architecture.
 * Type chosen based on alignment size restrictions from <sys/isa_defs.h>.
 * We desire to force up to (but no more than) 64-bit (8 byte) alignment,
 * on platforms where it is possible to do so. (e.g not possible on ia32).
 * For all currently supported platforms by our implementation
 * in <sys/isa_defs.h>, (i.e. sparc, sparcv9, ia32, ia64)
 * type "double" is suitable for that intent.
 *
 * Note: Type "double" is chosen over the more obvious integer type int64_t.
 *   int64_t is not a valid type for strict ANSI/ISO C compilation on ILP32.
 */
typedef double          sockaddr_maxalign_t;

#define _SS_ALIGNSIZE   (sizeof (sockaddr_maxalign_t))

/*
 * Definitions used for sockaddr_storage structure paddings design.
 */
#define _SS_PAD1SIZE    (_SS_ALIGNSIZE - sizeof (sa_family_t))
#define _SS_PAD2SIZE    (_SS_MAXSIZE - (sizeof (sa_family_t)+ \
                        _SS_PAD1SIZE + _SS_ALIGNSIZE))

struct sockaddr_storage {
  sa_family_t     ss_family;      /* Address family */
  /* Following fields are implementation specific */
  char            _ss_pad1[_SS_PAD1SIZE];
  sockaddr_maxalign_t _ss_align;
  char            _ss_pad2[_SS_PAD2SIZE];
};

/* For SIOC[GS]LIFLNKINFO */
typedef struct lif_ifinfo_req {
  uint8_t         lir_maxhops;
  uint32_t        lir_reachtime;          /* Reachable time in msec */
  uint32_t        lir_reachretrans;       /* Retransmission timer msec */
  uint32_t        lir_maxmtu;
} lif_ifinfo_req_t;

/* Interface name size limit */
#define LIFNAMSIZ	32

/*
 * For SIOCLIF*ND ioctls.
 *
 * The lnr_state_* fields use the ND_* neighbor reachability states.
 * The 3 different fields are for use with SIOCLIFSETND to cover the cases
 * when
 *      A new entry is created
 *      The entry already exists and the link-layer address is the same
 *      The entry already exists and the link-layer address differs
 *
 * Use ND_UNCHANGED and ND_ISROUTER_UNCHANGED to not change any state.
 */
#define ND_MAX_HDW_LEN  64
typedef struct lif_nd_req {
  struct sockaddr_storage lnr_addr;
  uint8_t                 lnr_state_create;       /* When creating */
  uint8_t                 lnr_state_same_lla;     /* Update same addr */
  uint8_t                 lnr_state_diff_lla;     /* Update w/ diff. */
  int                     lnr_hdw_len;
  int                     lnr_flags;              /* See below */
  /* padding because ia32 "long long"s are only 4-byte aligned. */
  int                     lnr_pad0;
  char                    lnr_hdw_addr[ND_MAX_HDW_LEN];
} lif_nd_req_t;

/* Interface name size limit */
#define LIFNAMSIZ       32

/*
 * Interface request structure used for socket
 * ioctl's.  All interface ioctl's must have parameter
 * definitions which begin with ifr_name.  The
 * remainder may be interface specific.
 * Note: This data structure uses 64bit type uint64_t which is not
 *       a valid type for strict ANSI/ISO C compilation for ILP32.
 *       Applications with ioctls using this structure that insist on
 *       building with strict ANSI/ISO C (-Xc) will need to be LP64.
 */
#if defined(_LP64) || (__STDC__ - 0 == 0 && !defined(_NO_LONGLONG))
struct  lifreq {
  char    lifr_name[LIFNAMSIZ];           /* if name, e.g. "en0" */
  union {
    int     lifru_addrlen;          /* for subnet/token etc */
    uint_t  lifru_ppa;              /* SIOCSLIFNAME */
  } lifr_lifru1;
#define lifr_addrlen    lifr_lifru1.lifru_addrlen
#define lifr_ppa        lifr_lifru1.lifru_ppa   /* Driver's ppa */
  uint_t  lifr_movetoindex;               /* FAILOVER/FAILBACK ifindex */
  union {
    struct  sockaddr_storage lifru_addr;
    struct  sockaddr_storage lifru_dstaddr;
    struct  sockaddr_storage lifru_broadaddr;
    struct  sockaddr_storage lifru_token;   /* With lifr_addrlen */
    struct  sockaddr_storage lifru_subnet;  /* With lifr_addrlen */
    int     lifru_index;            /* interface index */
    uint64_t lifru_flags;           /* Flags for SIOC?LIFFLAGS */
    int     lifru_metric;
    uint_t  lifru_mtu;
    char    lifru_data[1];          /* interface dependent data */
    char    lifru_enaddr[6];
    int     lif_muxid[2];           /* mux id's for arp and ip */
    struct lif_nd_req       lifru_nd_req;
    struct lif_ifinfo_req   lifru_ifinfo_req;
    char    lifru_groupname[LIFNAMSIZ]; /* SIOC[GS]LIFGROUPNAME */
    uint_t  lifru_delay;               /* SIOC[GS]LIFNOTIFYDELAY */
  } lifr_lifru;

#define lifr_addr       lifr_lifru.lifru_addr   /* address */
#define lifr_dstaddr    lifr_lifru.lifru_dstaddr /* other end of p-to-p link */
#define lifr_broadaddr  lifr_lifru.lifru_broadaddr /* broadcast address */
#define lifr_token      lifr_lifru.lifru_token  /* address token */
#define lifr_subnet     lifr_lifru.lifru_subnet /* subnet prefix */
#define lifr_index      lifr_lifru.lifru_index  /* interface index */
#define lifr_flags      lifr_lifru.lifru_flags  /* flags */
#define lifr_metric     lifr_lifru.lifru_metric /* metric */
#define lifr_mtu        lifr_lifru.lifru_mtu    /* mtu */
#define lifr_data       lifr_lifru.lifru_data   /* for use by interface */
#define lifr_enaddr     lifr_lifru.lifru_enaddr /* ethernet address */
#define lifr_index      lifr_lifru.lifru_index  /* interface index */
#define lifr_ip_muxid   lifr_lifru.lif_muxid[0]
#define lifr_arp_muxid  lifr_lifru.lif_muxid[1]
#define lifr_nd         lifr_lifru.lifru_nd_req /* SIOCLIF*ND */
#define lifr_ifinfo     lifr_lifru.lifru_ifinfo_req /* SIOC[GS]LIFLNKINFO */
#define lifr_groupname  lifr_lifru.lifru_groupname
#define lifr_delay      lifr_lifru.lifru_delay
};
#endif /* defined(_LP64) || (__STDC__ - 0 == 0 && !defined(_NO_LONGLONG)) */


/* Used by SIOCGLIFNUM. Uses same flags as in struct lifconf */
struct lifnum {
  sa_family_t     lifn_family;
  int             lifn_flags;     /* request specific interfaces */
  int             lifn_count;     /* Result */
};

/*
 * Structure used in SIOCGLIFCONF request.
 * Used to retrieve interface configuration
 * for machine (useful for programs which
 * must know all networks accessible) for a given address family.
 * Using AF_UNSPEC will retrieve all address families.
 */
struct  lifconf {
  sa_family_t     lifc_family;
  int             lifc_flags;     /* request specific interfaces */
  int             lifc_len;       /* size of associated buffer */
  union {
    caddr_t lifcu_buf;
    struct  lifreq *lifcu_req;
  } lifc_lifcu;
#define lifc_buf lifc_lifcu.lifcu_buf   /* buffer address */
#define lifc_req lifc_lifcu.lifcu_req   /* array of structures returned */
};

#ifndef _IOWRN
#define _IOWRN(x, y, t)                                                 \
            ((int)((uint32_t)(IOC_INOUT|(((t)&IOCPARM_MASK)<<16)| \
            (x<<8)|y)))
#endif

#define SIOCGLIFNUM     _IOWR('i', 130, struct lifnum)  /* get number of ifs */
#define SIOCGLIFCONF    _IOWRN('i', 120, 16)            /* get if list */
#define SIOCGLIFFLAGS   _IOWR('i', 117, struct lifreq)  /* get if flags */
#define SIOCGLIFINDEX   _IOWR('i', 133, struct lifreq)  /* get if index */

#endif
