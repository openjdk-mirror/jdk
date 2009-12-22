#ifndef __boneifs_h
#define __boneifs_h

typedef struct ifmedia_entry { 
        struct ifmedia_entry	*ifm_next; 
        int        				ifm_media;      /* description of this media attachment */ 
        int        				ifm_data;       /* for driver-specific use */ 
        void        			*ifm_aux;       /* for driver-specific use */ 
} ifmedia_entry_t; 

/* 
 * One of these goes into a network interface's softc structure. 
 * It is used to keep general media state. 
 */ 
typedef struct ifmedia { 
        int        ifm_mask;       /* mask of changes we don't care about */ 
        int        ifm_media;      /* current user-set media word */ 
        ifmedia_entry_t *ifm_cur;        /* currently selected media */ 
        ifmedia_entry_t	*ifm_list; ; /* list of all supported media */ 
} ifmedia_t; 


typedef struct if_data { 
        /* generic interface information */
		ifmedia_t	ifi_media;			/* media, see if_media.h */
        uint8  ifi_type;               /* ethernet, tokenring, etc */  
        uint8  ifi_addrlen;            /* media address length */ 
        uint8  ifi_hdrlen;             /* media header length */ 
        uint32  ifi_mtu;                /* maximum transmission unit */
										/* this does not count framing; */
										/* e.g. ethernet would be 1500 */ 
        uint32  ifi_metric;             /* routing metric (external only) */ 
        /* volatile statistics */ 
        uint32  ifi_ipackets;           /* packets received on interface */ 
        uint32  ifi_ierrors;            /* input errors on interface */ 
        uint32  ifi_opackets;           /* packets sent on interface */ 
        uint32  ifi_oerrors;            /* output errors on interface */ 
        uint32  ifi_collisions;         /* collisions on csma interfaces */ 
        uint32  ifi_ibytes;             /* total number of octets received */ 
        uint32  ifi_obytes;             /* total number of octets sent */ 
        uint32  ifi_imcasts;            /* packets received via multicast */ 
        uint32  ifi_omcasts;            /* packets sent via multicast */ 
        uint32  ifi_iqdrops;            /* dropped on input, this interface */ 
        uint32  ifi_noproto;            /* destined for unsupported protocol */ 
        bigtime_t ifi_lastchange; /* time of last administrative change */ 
} ifdata_t; 

typedef struct ifaddr {
		struct 			domain_info *domain;	/* address family of this address */
        struct        	sockaddr *ifa_addr;     /* address of interface */ 
        struct        	sockaddr *ifa_dstaddr;  /* other end of p-to-p link */ 
#define	ifa_broadaddr   ifa_dstaddr     		/* broadcast address interface */ 
        struct        	sockaddr *ifa_netmask;  /* used to determine subnet */ 
        struct        	lognet *ifa_ifp;        /* back-pointer to logical interface */ 
        struct        	ifaddr *ifa_next;       /* next address for domain */ 
} ifaddr_t; 

typedef unsigned int if_index_t;
#define IFNAMSIZ 32

typedef struct ifreq {
	char ifr_name[IFNAMSIZ];
	if_index_t		ifr_index;
	struct sockaddr ifr_addr;
	struct sockaddr ifr_mask;
	struct sockaddr ifr_dstaddr;
#define ifr_broadaddr ifr_dstaddr
	ifdata_t		ifr_data;
	uint32          ifr_flags;
	if_index_t		ifr_driver_index;
	uint8			ifr_hwaddr[256];
#define        ifr_mtu          ifr_data.ifi_mtu 
#define        ifr_type         ifr_data.ifi_type 
#define		   ifr_media    ifr_data.ifi_media 
#define        ifr_addrlen      ifr_data.ifi_addrlen 
#define        ifr_hdrlen       ifr_data.ifi_hdrlen 
#define        ifr_metric       ifr_data.ifi_metric 
#define        ifr_baudrate     ifr_data.ifi_baudrate 
#define        ifr_ipackets     ifr_data.ifi_ipackets 
#define        ifr_ierrors      ifr_data.ifi_ierrors 
#define        ifr_opackets     ifr_data.ifi_opackets 
#define        ifr_oerrors      ifr_data.ifi_oerrors 
#define        ifr_collisions   ifr_data.ifi_collisions 
#define        ifr_ibytes       ifr_data.ifi_ibytes 
#define        ifr_obytes       ifr_data.ifi_obytes 
#define        ifr_imcasts      ifr_data.ifi_imcasts 
#define        ifr_omcasts      ifr_data.ifi_omcasts 
#define        ifr_noproto      ifr_data.ifi_noproto 
#define        ifr_lastchange   ifr_data.ifi_lastchange 
} ifreq_t;


typedef struct ifconf {
	int ifc_len;
	union {
		void 		*ifcu_buf;
		ifreq_t		*ifcu_req;
		int32		ifcu_val;
	} ifc_ifcu;
#define ifc_buf ifc_ifcu.ifcu_buf
#define ifc_req ifc_ifcu.ifcu_req
#define ifc_val ifc_ifcu.ifcu_val
} ifconf_t;


#endif	//	__boneifs_h
