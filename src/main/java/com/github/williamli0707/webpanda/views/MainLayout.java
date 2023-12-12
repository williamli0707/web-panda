package com.github.williamli0707.webpanda.views;

import com.github.williamli0707.webpanda.WebPandaApplication;
import com.github.williamli0707.webpanda.security.SecurityService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Uses(MainView.class)
public class MainLayout extends AppLayout {
    private final SecurityService securityService;
    private H2 viewTitle;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        String u = securityService.getAuthenticatedUser().getUsername();
        Button logout = new Button("Log out", e -> securityService.logout());

        HorizontalLayout header = new HorizontalLayout(toggle, viewTitle, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(viewTitle);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(true, header);
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
