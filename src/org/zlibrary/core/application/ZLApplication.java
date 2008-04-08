package org.zlibrary.core.application;

import java.util.*;
import org.zlibrary.core.util.*;

import org.zlibrary.core.library.ZLibrary;
import org.zlibrary.core.options.ZLBooleanOption;
import org.zlibrary.core.options.ZLIntegerOption;
import org.zlibrary.core.options.ZLIntegerRangeOption;
import org.zlibrary.core.options.ZLOption;
import org.zlibrary.core.resources.ZLResource;
import org.zlibrary.core.view.ZLPaintContext;
import org.zlibrary.core.view.ZLView;
import org.zlibrary.core.view.ZLViewWidget;
import org.zlibrary.core.xml.ZLStringMap;
import org.zlibrary.core.xml.ZLXMLReaderAdapter;

public abstract class ZLApplication {
	private static final String MouseScrollUpKey = "<MouseScrollDown>";
	private static final String MouseScrollDownKey = "<MouseScrollUp>";
	public static final String NoAction = "none";
	
	private static final String ROTATION = "Rotation";
	private static final String ANGLE = "Angle";
	private static final String STATE = "State";
	private static final String KEYBOARD = "Keyboard";
	private static final String FULL_CONTROL = "FullControl";
	private static final String CONFIG = "Config";
	private static final String AUTO_SAVE = "AutoSave";
	private static final String TIMEOUT = "Timeout";

	public final ZLIntegerOption RotationAngleOption =
		// temporary commented while we have no options dialog
		//new ZLIntegerOption(ZLOption.CONFIG_CATEGORY, ROTATION, ANGLE, ZLViewWidget.Angle.DEGREES90);
		new ZLIntegerOption(ZLOption.CONFIG_CATEGORY, ROTATION, ANGLE, -1);
	public final ZLIntegerOption AngleStateOption =
		new ZLIntegerOption(ZLOption.CONFIG_CATEGORY, STATE, ANGLE, ZLViewWidget.Angle.DEGREES0);	

	public final ZLBooleanOption KeyboardControlOption =
		new ZLBooleanOption(ZLOption.CONFIG_CATEGORY, KEYBOARD, FULL_CONTROL, false);

	public final ZLBooleanOption ConfigAutoSavingOption =
		new ZLBooleanOption(ZLOption.CONFIG_CATEGORY, CONFIG, AUTO_SAVE, true);
	public final ZLIntegerRangeOption ConfigAutoSaveTimeoutOption =
		new ZLIntegerRangeOption(ZLOption.CONFIG_CATEGORY, CONFIG, TIMEOUT, 1, 6000, 30);

	public final ZLIntegerRangeOption KeyDelayOption =
		new ZLIntegerRangeOption(ZLOption.CONFIG_CATEGORY, "Options", "KeyDelay", 0, 5000, 250);
	
	private final ZLPaintContext myContext;
	protected ZLViewWidget myViewWidget;
	private ZLApplicationWindow myWindow;
	private ZLView myInitialView;

	private final HashMap myIdToActionMap = new HashMap();
	private Toolbar myToolbar;
	private Menubar myMenubar;
	//private ZLTime myLastKeyActionTime;
	//private ZLMessageHandler myPresentWindowHandler;

	protected ZLApplication() {
		myContext = ZLibrary.getInstance().createPaintContext();
		
		if (ConfigAutoSavingOption.getValue()) {
			//ZLOption.startAutoSave(ConfigAutoSaveTimeoutOption.getValue());
		}

		//myPresentWindowHandler = new PresentWindowHandler(this);
		//ZLCommunicationManager.instance().registerHandler("present", myPresentWindowHandler);

		new ToolbarCreator().read(ZLibrary.JAR_DATA_PREFIX + "data/default/toolbar.xml");
		new MenubarCreator().read(ZLibrary.JAR_DATA_PREFIX + "data/default/menubar.xml");
	}
	
	final Toolbar getToolbar() {
		return myToolbar;
	}

	final Menubar getMenubar() {
		return myMenubar;
	}

	protected final void setView(ZLView view) {
		if (view != null) {
			if (myViewWidget != null) {
				myViewWidget.setView(view);
				resetWindowCaption();
				refreshWindow();
			} else {
				myInitialView = view;
			}
		}
	}

	protected final ZLView getCurrentView() {
		return (myViewWidget != null) ? myViewWidget.getView() : null;
	}

	public final void quit() {
		if (myWindow != null) {
			myWindow.close();
		}
	}

	abstract protected void onQuit();

	final void setWindow(ZLApplicationWindow window) {
		myWindow = window;
	}

	public void initWindow() {
		setViewWidget(myWindow.createViewWidget());
		if (KeyboardControlOption.getValue()) {
			grabAllKeys(true);
		}
		myWindow.init();
		setView(myInitialView);
	}

	public final ZLPaintContext getContext() {
		return myContext;
	}

	public final void refreshWindow() {
		if (myViewWidget != null) {
			myViewWidget.repaint();
		}
		if (myWindow != null) {
			myWindow.refresh();
		}
	}

	protected final void resetWindowCaption() {
		if (myWindow != null) {
			ZLView view = getCurrentView();
			if (view != null) {
				myWindow.setCaption(ZLibrary.getInstance().getApplicationName() + " - " + view.getCaption());
			} else {
				myWindow.setCaption(ZLibrary.getInstance().getApplicationName());
			}
		}
	}
	
	public final void setFullscreen(boolean fullscreen) {
		if (myWindow != null) {
			myWindow.setFullscreen(fullscreen);
		}
	}
	
	public final boolean isFullscreen() {
		return (myWindow != null) && myWindow.isFullscreen();
	}
	
	protected final void addAction(String actionId, ZLAction action) {
		myIdToActionMap.put(actionId, action);
	}

	public final void grabAllKeys(boolean grab) {
		if (myWindow != null) {
			//myWindow.grabAllKeys(grab);
		}
	}

	public final void trackStylus(boolean track) {
		if (myViewWidget != null) {
			myViewWidget.trackStylus(track);
		}
	}

	public final void setHyperlinkCursor(boolean hyperlink) {
		if (myWindow != null) {
			//myWindow.setHyperlinkCursor(hyperlink);
		}
	}

	private final ZLAction getAction(String actionId) {
		return (ZLAction)myIdToActionMap.get(actionId);
	}
	
	public final boolean isActionVisible(String actionId) {
		ZLAction action = getAction(actionId);
		return (action != null) && action.isVisible();
	}
	
	public final boolean isActionEnabled(String actionId) {
		ZLAction action = getAction(actionId);
		return (action != null) && action.isEnabled();
	}
	
	public final void doAction(String actionId) {
		ZLAction action = getAction(actionId);
		if (action != null) {
			action.checkAndRun();
		}
	}
	//may be protected
	abstract public ZLKeyBindings keyBindings();
	
	public final void doActionByKey(String key) {		
		String actionId = keyBindings().getBinding(key);
		if (actionId != null) {
			ZLAction a = getAction(keyBindings().getBinding(key));
			if ((a != null)){// &&
					//(!a.useKeyDelay() /*||
					 //(myLastKeyActionTime.millisecondsTo(ZLTime()) >= KeyDelayOption.getValue())*/)) {
				a.checkAndRun();
				//myLastKeyActionTime = ZLTime();
			}
		}
	}

	public boolean closeView() {
		quit();
		return true;
	}
	
	public void openFile(String fileName) {
		// TODO: implement or change to abstract
	}

	public final void presentWindow() {
		if (myWindow != null) {
			//myWindow.present();
		}
	}

	//public String lastCaller() {
		//return null;//((PresentWindowHandler)myPresentWindowHandler).lastCaller();
	//}
	
	//public void resetLastCaller() {
		//((PresentWindowHandler)myPresentWindowHandler).resetLastCaller();
	//}

	final void setViewWidget(ZLViewWidget viewWidget) {
		myViewWidget = viewWidget;
	}

	//Action
	static abstract public class ZLAction {
		public boolean isVisible() {
			return true;
		}

		public boolean isEnabled() {
			return isVisible();
		}
		
		public final void checkAndRun() {
			if (isEnabled()) {
				run();
			}
		}
		
		public boolean useKeyDelay() {
			return true;
		}
		
		abstract protected void run();
	}

	//full screen action
	protected static class FullscreenAction extends ZLAction {
		private final ZLApplication myApplication;
		private	final boolean myIsToggle;

		public FullscreenAction(ZLApplication application, boolean toggle) {
			myApplication = application;
			myIsToggle = toggle;
		}
		
		public boolean isVisible() {
			return myIsToggle || !myApplication.isFullscreen();
		}
		
		public void run() {
			myApplication.setFullscreen(!myApplication.isFullscreen());
		}
	}

	//rotation action
	protected static final class RotationAction extends ZLAction {
		private ZLApplication myApplication;

		public RotationAction(ZLApplication application) {
			myApplication = application;
		}
		
		public boolean isVisible() {
			return (myApplication.myViewWidget != null) &&
			 ((myApplication.RotationAngleOption.getValue() != ZLViewWidget.Angle.DEGREES0) ||
				(myApplication.myViewWidget.getRotation() != ZLViewWidget.Angle.DEGREES0));
		}
		
		public void run() {
			int optionValue = myApplication.RotationAngleOption.getValue();
			int oldAngle = myApplication.myViewWidget.getRotation();
			int newAngle = ZLViewWidget.Angle.DEGREES0;
			if (optionValue == -1) {
				newAngle = (oldAngle + 90) % 360;
			} else {
				newAngle = (oldAngle == ZLViewWidget.Angle.DEGREES0) ?
					optionValue : ZLViewWidget.Angle.DEGREES0;
			}
			myApplication.myViewWidget.rotate(newAngle);
			myApplication.AngleStateOption.setValue(newAngle);
			myApplication.refreshWindow();		
		}
	}
	
	//toolbar
	static public final class Toolbar {
		private final ArrayList myItems = new ArrayList();
		private final ZLResource myResource = ZLResource.resource("toolbar");

		private void addButton(String actionId/*, ButtonGroup group*/) {
			ButtonItem button = new ButtonItem(actionId, myResource.getResource(actionId));
			myItems.add(button);
			//button.setButtonGroup(group);
		}
		
		/*
		ButtonGroup createButtonGroup(int unselectAllButtonsActionId) {
			return new ButtonGroup(unselectAllButtonsActionId);
		}
		
		public void addOptionEntry(ZLOptionEntry entry) {
			if (entry != null) {
				myItems.add(new OptionEntryItem(entry));
			}
		}
		*/
		
		public void addSeparator() {
			myItems.add(new SeparatorItem());
		}

		int size() {
			return myItems.size();
		}

		Item getItem(int index) {
			return (Item)myItems.get(index);
		}

		public interface Item {
		}
		
		public final class ButtonItem implements Item {
			private final String myActionId;
			private final ZLResource myTooltip;
			//private ButtonGroup myButtonGroup;
			
			public ButtonItem(String actionId, ZLResource tooltip) {
				myActionId = actionId;
				myTooltip = tooltip;
			}

			public String getActionId() {
				return myActionId;
			}
			
			public String getIconName() {
				return myActionId;
			}
			
			public String getTooltip() {
				return myTooltip.hasValue() ? myTooltip.getValue() : null;
			}

			/*
			ButtonGroup getButtonGroup() {
				return myButtonGroup;
			}
			
			boolean isToggleButton() {
				return myButtonGroup != null;
			}
			
			void press() {
				if (isToggleButton()) {
					myButtonGroup.press(this);
				}
			}
			
			boolean isPressed() {
				return isToggleButton() && (this == myButtonGroup.PressedItem);
			}

			private void setButtonGroup(ButtonGroup bg) {
				if (myButtonGroup != null) {
					myButtonGroup.Items.remove(this);
				}
				
				myButtonGroup = bg;
				
				if (myButtonGroup != null) {
					myButtonGroup.Items.add(this);
				}
			}	
			*/
		}
		
		public class SeparatorItem implements Item {
		}
		
		/*
		public class OptionEntryItem implements Item {
			private ZLOptionEntry myOptionEntry;

			public OptionEntryItem(ZLOptionEntry entry) {
				myOptionEntry = entry;
			}
			
			public ZLOptionEntry entry() {
				return myOptionEntry;
			}
		}

		public final class ButtonGroup {
			public final int UnselectAllButtonsActionId;
			public final ArrayList<ButtonItem> Items = new ArrayList<ButtonItem>();
			public ButtonItem PressedItem;

			ButtonGroup(int unselectAllButtonsActionId) {
				UnselectAllButtonsActionId = unselectAllButtonsActionId;
				PressedItem = null;
			}
			
			void press(ButtonItem item) {
				PressedItem = item;
			}
		}
		*/
	}
	
	//Menu
	static class Menu {
		public interface Item {
		}

		private final ArrayList myItems = new ArrayList();
		private final ZLResource myResource;

		Menu(ZLResource resource) {
			myResource = resource;
		}

		ZLResource getResource() {
			return myResource;
		}

		void addItem(String actionId) {
			myItems.add(new Menubar.PlainItem(myResource.getResource(actionId).getValue(), actionId));
		}
		
		void addSeparator() {
			myItems.add(new Menubar.Separator());
		}
		
		Menubar.Submenu addSubmenu(String key) {
			Menubar.Submenu submenu = new Menubar.Submenu(myResource.getResource(key));
			myItems.add(submenu);
			return submenu;
		}

		int size() {
			return myItems.size();
		}

		Item getItem(int index) {
			return (Item)myItems.get(index);
		}
	}
	
	//MenuBar
	public static final class Menubar extends Menu {
		public static final class PlainItem implements Item {
			private final String myName;
			private final String myActionId;

			public PlainItem(String name, String actionId) {
				myName = name;
				myActionId = actionId;
			}

			public String getName() {
				return myName;
			}
			
			public String getActionId() {
				return myActionId;
			}
		};

		public static final class Submenu extends Menu implements Item {
			public Submenu(ZLResource resource) {
				super(resource);
			}

			public String getMenuName() {
				return getResource().getValue();
			}
		};
		
		public static final class Separator implements Item {
		};
			
		public Menubar() {
			super(ZLResource.resource("menu"));
		}
	}

	//MenuVisitor
	static public abstract class MenuVisitor {
		public final void processMenu(ZLApplication application) {
			if (application.myMenubar != null) {
				processMenu(application.myMenubar);
			}
		}

		private final void processMenu(Menu menu) {
			final int size = menu.size();
			for (int i = 0; i < size; ++i) {
				final Menu.Item item = menu.getItem(i);
				if (item instanceof Menubar.PlainItem) {
					processItem((Menubar.PlainItem)item);
				} else if (item instanceof Menubar.Submenu) {
					Menubar.Submenu submenu = (Menubar.Submenu)item;
					processSubmenuBeforeItems(submenu);
					processMenu(submenu);
					processSubmenuAfterItems(submenu);
				} else if (item instanceof Menubar.Separator) {
					processSepartor((Menubar.Separator)item);
				}
			}
		}

		protected abstract void processSubmenuBeforeItems(Menubar.Submenu submenu);
		protected abstract void processSubmenuAfterItems(Menubar.Submenu submenu);
		protected abstract void processItem(Menubar.PlainItem item);
		protected abstract void processSepartor(Menubar.Separator separator);
	}
	
	static public class PresentWindowHandler {//extends ZLMessageHandler {
		private ZLApplication myApplication;
		private String myLastCaller;

		//public PresentWindowHandler(ZLApplication application);
		//public void onMessageReceived(List<String> arguments);
		//public String lastCaller();
		//public void resetLastCaller();
	}

	private class ToolbarCreator extends ZLXMLReaderAdapter {
		private static final String BUTTON = "button";
		private static final String SEPARATOR = "separator";

		public boolean startElementHandler(String tag, ZLStringMap attributes) {
			if (myToolbar == null) {
				myToolbar = new Toolbar();
			}
			if (BUTTON == tag) {
				String id = attributes.getValue("id");
				if (id != null) {
					myToolbar.addButton(id);
				}
			} else if (SEPARATOR == tag) {
				myToolbar.addSeparator();
			}
			return false;
		}
	}

	private class MenubarCreator extends ZLXMLReaderAdapter {
		private static final String ITEM = "item";
		private static final String SUBMENU = "submenu";

		private final ArrayList mySubmenuStack = new ArrayList();

		public boolean startElementHandler(String tag, ZLStringMap attributes) {
			if (myMenubar == null) {
				myMenubar = new Menubar();
			}
			final ArrayList stack = mySubmenuStack;
			Menu menu = stack.isEmpty() ? myMenubar : (Menu)stack.get(stack.size() - 1);
			if (ITEM == tag) {
				String id = attributes.getValue("id");
				if (id != null) {
					menu.addItem(id);
				}
			} else if (SUBMENU == tag) {
				String id = attributes.getValue("id");
				if (id != null) {
					stack.add(menu.addSubmenu(id));
				}
			}
			return false;
		}

		public boolean endElementHandler(String tag) {
			if (SUBMENU == tag) {
				final ArrayList stack = mySubmenuStack;
				if (!stack.isEmpty()) {
					stack.remove(stack.size() - 1);
				}
			}
			return false;
		}
	}
}
