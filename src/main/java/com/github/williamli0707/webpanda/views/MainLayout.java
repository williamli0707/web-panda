package com.github.williamli0707.webpanda.views;

import com.github.williamli0707.webpanda.WebPandaApplication;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Uses(MainView.class)
public class MainLayout extends AppLayout {

    private H2 viewTitle;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H1 appName = new H1("Web-PANDA");
//        Image icon = new Image("images/web-panda.png", "web panda");
        Image icon = new Image("images/web-panda.png", "web panda");
//        File file = new File("src/main/resources/images/web-panda.png");
//        Image icon = new Image(new StreamResource(file,
//                () -> getClass().getClassLoader().getResourceAsStream("images/web-panda.png")), "web panda");
        icon.setWidth("250px");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(icon, header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Analyzer", MainView.class/*, LineAwesomeIcon.GLOBE_SOLID.create()*/));
        nav.addItem(new SideNavItem("History", HistoryView.class/*, LineAwesomeIcon.FILE.create()*/));
        nav.addItem(new SideNavItem("Settings", SettingsView.class/*, LineAwesomeIcon.FILE.create()*/));
        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();
        layout.add(new NativeLabel("Version " + WebPandaApplication.version));

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
