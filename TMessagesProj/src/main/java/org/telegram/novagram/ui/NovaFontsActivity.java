package org.telegram.novagram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.novagram.NovaConfig;
import org.telegram.novagram.design.NovaFontManager;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class NovaFontsActivity extends UniversalFragment {

    private static final int REQ_PICK_FONT = 9421;

    private static final int ID_FONT_SYSTEM = 1;
    private static final int ID_FONT_SANS_MEDIUM = 2;
    private static final int ID_FONT_CONDENSED = 3;
    private static final int ID_FONT_SERIF = 4;
    private static final int ID_FONT_MONO = 5;
    private static final int ID_FONT_CUSTOM = 6;
    private static final int ID_IMPORT_FONT = 7;

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        fragmentView = super.createView(context);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        return fragmentView;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.NovaFontsTitle);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int current = NovaConfig.getFontPreset();

        items.add(UItem.asHeader(getString(R.string.NovaFontsBuiltIn)));
        items.add(UItem.asRadio(ID_FONT_SYSTEM, getString(R.string.NovaFontSystem))
                .setChecked(current == NovaConfig.FONT_SYSTEM));
        items.add(UItem.asRadio(ID_FONT_SANS_MEDIUM, getString(R.string.NovaFontSansMedium))
                .setChecked(current == NovaConfig.FONT_SANS_MEDIUM));
        items.add(UItem.asRadio(ID_FONT_CONDENSED, getString(R.string.NovaFontCondensed))
                .setChecked(current == NovaConfig.FONT_CONDENSED));
        items.add(UItem.asRadio(ID_FONT_SERIF, getString(R.string.NovaFontSerif))
                .setChecked(current == NovaConfig.FONT_SERIF));
        items.add(UItem.asRadio(ID_FONT_MONO, getString(R.string.NovaFontMono))
                .setChecked(current == NovaConfig.FONT_MONO));

        if (!TextUtils.isEmpty(NovaConfig.getCustomFontPath())) {
            String name = !TextUtils.isEmpty(NovaConfig.getCustomFontName())
                    ? NovaConfig.getCustomFontName()
                    : "Custom Font";
            items.add(UItem.asRadio(ID_FONT_CUSTOM, name)
                    .setChecked(current == NovaConfig.FONT_CUSTOM));
        }

        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.NovaFontCustomHeader)));
        items.add(UItem.asButton(ID_IMPORT_FONT, R.drawable.msg_add, getString(R.string.NovaFontImport)).accent());
        items.add(UItem.asShadow(getString(R.string.NovaFontImportHint)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_FONT_SYSTEM:
                NovaFontManager.selectPreset(NovaConfig.FONT_SYSTEM);
                break;
            case ID_FONT_SANS_MEDIUM:
                NovaFontManager.selectPreset(NovaConfig.FONT_SANS_MEDIUM);
                break;
            case ID_FONT_CONDENSED:
                NovaFontManager.selectPreset(NovaConfig.FONT_CONDENSED);
                break;
            case ID_FONT_SERIF:
                NovaFontManager.selectPreset(NovaConfig.FONT_SERIF);
                break;
            case ID_FONT_MONO:
                NovaFontManager.selectPreset(NovaConfig.FONT_MONO);
                break;
            case ID_FONT_CUSTOM:
                NovaFontManager.selectPreset(NovaConfig.FONT_CUSTOM);
                break;
            case ID_IMPORT_FONT:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQ_PICK_FONT);
                return;
            default:
                return;
        }
        listView.adapter.update(true);
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FONT && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            String installed = NovaFontManager.installFontFromUri(getParentActivity(), data.getData());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (!TextUtils.isEmpty(installed)) {
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.chats_infotip,
                        LocaleController.formatString(R.string.NovaFontInstalled, installed)
                ).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(getString(R.string.NovaFontInstallFailed)).show();
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
