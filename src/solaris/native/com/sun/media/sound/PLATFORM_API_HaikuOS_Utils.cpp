/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

DeviceCache::DeviceCache()
{
	//BMediaRoster* roster = BMediaRoster::Roster();
	//roster->StartWatching(BMessenger(this));

	_Refresh();
}

int
DeviceCache::DeviceCount()
{
	int count = -1;
	if (lock.Lock()) {
		count = nodes.CountItems();
		lock.Unlock();
	}

	return count;
}

status_t
DeviceCache::GetDevice(int index, live_node_info* _nodeInfo)
{
	if (!lock.Lock())
		return B_ERROR;

	live_node_info* nodeInfo = nodes.ItemAt(index);
	lock.Unlock();

	if (nodeInfo != NULL) {
		*_nodeInfo = nodeInfo;
		return B_OK;
	}

	return B_ERROR;
}

void
DeviceCache::MessageReceived(BMessage* msg)
{
	_Refresh();
}

void
DeviceCache::_Refresh()
{
	nodes.MakeEmpty();
	static const int maxCount = 128;

	live_node_info liveNodes[maxCount];
	int32 count = maxCount;

	media_format nodeFormat;
	nodeFormat.type = B_MEDIA_RAW_AUDIO;

	roster->GetLiveNodes(liveNodes, &count, &nodeFormat, NULL, NULL, B_PHYSICAL_OUTPUT);

	for (int i = 0; i < count; i++) {
		nodes.push_back(liveNodes[i]);
	}

	count = maxCount;
	roster->GetLiveNodes(liveNodes, &count, NULL, &nodeFormat, NULL, B_PHYSICAL_INPUT);

	for (int i = 0; i < count; i++) {
		if (std::find(nodes.begin(), nodes.end(), liveNodes[i]) != nodes.end()) {
			nodes.push_back(liveNodes[i]);
		}
	}
}
