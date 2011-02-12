/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.network.authentication.litres;

import java.util.*;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;

import org.geometerplus.fbreader.network.*;

abstract class SortedCatalogItem extends NetworkCatalogItem {
	private final List<NetworkLibraryItem> myChildren = new LinkedList<NetworkLibraryItem>();

	private SortedCatalogItem(NetworkCatalogItem parent, ZLResource resource, List<NetworkLibraryItem> children) {
		super(parent.Link, resource.getValue(), resource.getResource("summary").getValue(), "", parent.URLByType);
		for (NetworkLibraryItem child : children) {
			if (accepts(child)) {
				myChildren.add(child);
			}
		}
		final Comparator<NetworkLibraryItem> comparator = getComparator();
		if (comparator != null) {
			Collections.sort(myChildren, comparator);
		}
	}

	public boolean isEmpty() {
		return myChildren.isEmpty();
	}

	protected abstract Comparator<NetworkLibraryItem> getComparator();
	protected boolean accepts(NetworkLibraryItem item) {
		return item instanceof NetworkBookItem;
	}

	public SortedCatalogItem(NetworkCatalogItem parent, String resourceKey, List<NetworkLibraryItem> children) {
		this(parent, ZLResource.resource("networkView").getResource(resourceKey), children);
	}

	@Override
	public void onDisplayItem() {
	}

	@Override
	public void loadChildren(NetworkOperationData.OnNewItemListener listener) throws ZLNetworkException {
		for (NetworkLibraryItem child : myChildren) {
			listener.onNewItem(Link, child);
		}
		listener.commitItems(Link);
	}
}

class ByAuthorCatalogItem extends SortedCatalogItem {
	ByAuthorCatalogItem(NetworkCatalogItem parent, List<NetworkLibraryItem> children) {
		super(parent, "byAuthor", children);
	}

	@Override
	protected Comparator<NetworkLibraryItem> getComparator() {
		return new NetworkBookItemComparator();
	}
}

class ByTitleCatalogItem extends SortedCatalogItem {
	ByTitleCatalogItem(NetworkCatalogItem parent, List<NetworkLibraryItem> children) {
		super(parent, "byTitle", children);
	}

	@Override
	protected Comparator<NetworkLibraryItem> getComparator() {
		return new Comparator<NetworkLibraryItem>() {
			public int compare(NetworkLibraryItem item0, NetworkLibraryItem item1) {
				return item0.Title.compareTo(item1.Title);
			}
		};
	}
}

class ByDateCatalogItem extends SortedCatalogItem {
	ByDateCatalogItem(NetworkCatalogItem parent, List<NetworkLibraryItem> children) {
		super(parent, "byDate", children);
	}

	@Override
	protected Comparator<NetworkLibraryItem> getComparator() {
		return new Comparator<NetworkLibraryItem>() {
			public int compare(NetworkLibraryItem item0, NetworkLibraryItem item1) {
				return 0;
			}
		};
	}
}

class BySeriesCatalogItem extends SortedCatalogItem {
	BySeriesCatalogItem(NetworkCatalogItem parent, List<NetworkLibraryItem> children) {
		super(parent, "bySeries", children);
	}

	@Override
	protected Comparator<NetworkLibraryItem> getComparator() {
		return new Comparator<NetworkLibraryItem>() {
			public int compare(NetworkLibraryItem item0, NetworkLibraryItem item1) {
				final NetworkBookItem book0 = (NetworkBookItem)item0;
				final NetworkBookItem book1 = (NetworkBookItem)item1;
				int diff = book0.SeriesTitle.compareTo(book1.SeriesTitle);
				if (diff == 0) {
					diff = book0.IndexInSeries - book1.IndexInSeries;
				}
				return diff != 0 ? diff : book0.Title.compareTo(book1.Title);
			}
		};
	}

	@Override
	protected boolean accepts(NetworkLibraryItem item) {
		return
			item instanceof NetworkBookItem &&
			((NetworkBookItem)item).SeriesTitle != null;
	}
}

public class LitResBookshelfItem extends NetworkCatalogItem {
	private boolean myForceReload;

	public LitResBookshelfItem(INetworkLink link, String title, String summary, String cover, Map<Integer, String> urlByType, Accessibility accessibility) {
		super(link, title, summary, cover, urlByType, accessibility, CatalogType.OTHER);
	}

	@Override
	public void onDisplayItem() {
		myForceReload = false;
	}

	@Override
	public void loadChildren(NetworkOperationData.OnNewItemListener listener) throws ZLNetworkException {
		final LitResAuthenticationManager mgr =
			(LitResAuthenticationManager)Link.authenticationManager();

		// TODO: Maybe it's better to call isAuthorised(true) directly 
		// and let exception fly through???
		if (!mgr.mayBeAuthorised(true)) {
			throw new ZLNetworkException(NetworkException.ERROR_AUTHENTICATION_FAILED);
		}
		try {
			if (myForceReload) {
				mgr.reloadPurchasedBooks();
			}
		} finally {
			myForceReload = true;
			// TODO: implement asynchronous loading
			ArrayList<NetworkLibraryItem> children =
				new ArrayList<NetworkLibraryItem>(mgr.purchasedBooks());
			if (children.size() <= 5) {
				Collections.sort(children, new NetworkBookItemComparator());
				for (NetworkLibraryItem item : children) {
					listener.onNewItem(Link, item);
				}
			} else {
				listener.onNewItem(Link, new ByDateCatalogItem(this, children));
				listener.onNewItem(Link, new ByAuthorCatalogItem(this, children));
				listener.onNewItem(Link, new ByTitleCatalogItem(this, children));
				final BySeriesCatalogItem bySeries = new BySeriesCatalogItem(this, children);
				if (!bySeries.isEmpty()) {
					listener.onNewItem(Link, bySeries);
				}
			}
			listener.commitItems(Link);
		}
	}
}
