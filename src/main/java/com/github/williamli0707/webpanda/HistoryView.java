package com.github.williamli0707.webpanda;

import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Uses(DrawerToggle.class)
@Uses(SideNav.class)
@Uses(SideNavItem.class)
@Uses(Scroller.class)
@Uses(MainView.class)
@Uses(HistoryView.class)
@Uses(MainLayout.class)
@PageTitle("History Viewer")
@Route(value = "history", layout = MainLayout.class)
public class HistoryView extends VerticalLayout {
    public HistoryView() {


    }
}

/*
List most (highest scoring?) students, show a difference between iterations and what was weird about it
 */