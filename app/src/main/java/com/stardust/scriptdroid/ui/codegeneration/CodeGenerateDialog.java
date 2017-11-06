package com.stardust.scriptdroid.ui.codegeneration;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.ParentViewHolder;
import com.bignerdranch.expandablerecyclerview.model.Parent;
import com.stardust.autojs.codegeneration.UiSelectorGenerator;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.ui.widget.CheckBoxCompat;
import com.stardust.theme.dialog.ThemeColorMaterialDialogBuilder;
import com.stardust.theme.util.ListBuilder;
import com.stardust.util.ClipboardUtil;
import com.stardust.view.accessibility.NodeInfo;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

/**
 * Created by Stardust on 2017/11/6.
 */

public class CodeGenerateDialog extends ThemeColorMaterialDialogBuilder {

    private final List<OptionGroup> mOptionGroups = new ListBuilder<OptionGroup>()
            .add(new OptionGroup(R.string.text_options, false)
                    .addOption(R.string.text_using_id_selector, true)
                    .addOption(R.string.text_using_text_selector, true)
                    .addOption(R.string.text_using_desc_selector, true))
            .add(new OptionGroup(R.string.text_select)
                    .addOption(R.string.text_find_one, true)
                    .addOption(R.string.text_until_find)
                    .addOption(R.string.text_wait_for)
                    .addOption(R.string.text_selector_exists))
            .add(new OptionGroup(R.string.text_action)
                    .addOption(R.string.text_click)
                    .addOption(R.string.text_long_click)
                    .addOption(R.string.text_set_text)
                    .addOption(R.string.text_scroll_forward)
                    .addOption(R.string.text_scroll_backward))
            .list();

    @BindView(R.id.options)
    RecyclerView mOptionsRecyclerView;

    private NodeInfo mRootNode;
    private NodeInfo mTargetNode;
    private Adapter mAdapter;

    public CodeGenerateDialog(@NonNull Context context, NodeInfo rootNode, NodeInfo targetNode) {
        super(context);
        mRootNode = rootNode;
        mTargetNode = targetNode;
        positiveText(R.string.text_generate_and_copy);
        neutralText(R.string.text_generate_and_open_editor);
        onPositive(((dialog, which) -> generateCodeAndCopy()));
        onNeutral(((dialog, which) -> generateCodeAndOpenEditor()));
        setupViews();
    }

    private void generateCodeAndCopy() {
        String code = generateCode();
        if (code == null) {
            Toast.makeText(getContext(), R.string.text_generate_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardUtil.setClip(getContext(), code);
        Toast.makeText(getContext(), R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show();
    }

    private void generateCodeAndOpenEditor() {

    }

    private String generateCode() {
        UiSelectorGenerator generator = new UiSelectorGenerator(mRootNode, mTargetNode);
        OptionGroup settings = getOptionGroup(R.string.text_options);
        generator.setUsingId(settings.getOption(R.string.text_using_id_selector).checked);
        generator.setUsingText(settings.getOption(R.string.text_using_text_selector).checked);
        generator.setUsingDesc(settings.getOption(R.string.text_using_desc_selector).checked);
        generator.setSearchMode(getSearchMode());
        setAction(generator);
        return generator.generate();
    }

    private void setAction(UiSelectorGenerator generator) {
        OptionGroup action = getOptionGroup(R.string.text_action);
        if (action.getOption(R.string.text_click).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        }
        if (action.getOption(R.string.text_long_click).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
        }
        if (action.getOption(R.string.text_scroll_forward).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
        }
        if (action.getOption(R.string.text_scroll_backward).checked) {
            generator.setAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
        }
    }

    private int getSearchMode() {
        OptionGroup selectMode = getOptionGroup(R.string.text_select);
        if (selectMode.getOption(R.string.text_find_one).checked) {
            return UiSelectorGenerator.FIND_ONE;
        }
        if (selectMode.getOption(R.string.text_until_find).checked) {
            return UiSelectorGenerator.UNTIL_FIND;
        }
        if (selectMode.getOption(R.string.text_wait_for).checked) {
            return UiSelectorGenerator.WAIT_FOR;
        }
        if (selectMode.getOption(R.string.text_wait_for).checked) {
            return UiSelectorGenerator.EXISTS;
        }
        return UiSelectorGenerator.FIND_ONE;
    }

    private void setupViews() {
        View view = View.inflate(context, R.layout.dialog_code_generate, null);
        ButterKnife.bind(this, view);
        customView(view, false);
        mOptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new Adapter(mOptionGroups);
        mOptionsRecyclerView.setAdapter(mAdapter);
    }


    private OptionGroup getOptionGroup(int title) {
        for (OptionGroup group : mOptionGroups) {
            if (group.titleRes == title) {
                return group;
            }
        }
        throw new IllegalArgumentException();
    }


    private void uncheckOthers(int parentAdapterPosition, Option child) {
        boolean notify = false;
        for (Option other : child.group.options) {
            if (other != child) {
                if (other.checked) {
                    other.checked = false;
                    notify = true;
                }
            }
        }
        if (notify)
            mAdapter.notifyParentChanged(parentAdapterPosition);
    }

    private static class Option {
        int titleRes;
        boolean checked;
        OptionGroup group;

        Option(int titleRes, boolean checked) {
            this.titleRes = titleRes;
            this.checked = checked;
        }

    }

    class OptionViewHolder extends ChildViewHolder<Option> {

        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.checkbox)
        CheckBoxCompat checkBox;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(view -> checkBox.toggle());
        }

        @OnCheckedChanged(R.id.checkbox)
        void onCheckedChanged() {
            getChild().checked = checkBox.isChecked();
            if (checkBox.isChecked() && getChild().group.titleRes != R.string.text_action)
                uncheckOthers(getParentAdapterPosition(), getChild());
        }

    }

    private static class OptionGroup implements Parent<Option> {
        int titleRes;
        List<Option> options = new ArrayList<>();
        private final boolean mInitialExpanded;


        OptionGroup(int titleRes, boolean initialExpanded) {
            this.titleRes = titleRes;
            mInitialExpanded = initialExpanded;
        }

        OptionGroup(int titleRes) {
            this(titleRes, true);
        }

        Option getOption(int titleRes) {
            for (Option option : options) {
                if (option.titleRes == titleRes) {
                    return option;
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public List<Option> getChildList() {
            return options;
        }

        @Override
        public boolean isInitiallyExpanded() {
            return mInitialExpanded;
        }

        OptionGroup addOption(int titleRes) {
            return addOption(titleRes, false);
        }

        OptionGroup addOption(int res, boolean checked) {
            Option option = new Option(res, checked);
            option.group = this;
            options.add(option);
            return this;
        }
    }


    private class OptionGroupViewHolder extends ParentViewHolder<OptionGroup, Option> {

        TextView title;
        ImageView icon;

        OptionGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(view -> {
                if (isExpanded()) {
                    collapseView();
                } else {
                    expandView();
                }
            });
        }

        @Override
        public void onExpansionToggled(boolean expanded) {
            icon.setRotation(expanded ? -90 : 0);
        }
    }

    private class Adapter extends ExpandableRecyclerAdapter<OptionGroup, Option, OptionGroupViewHolder, OptionViewHolder> {

        public Adapter(@NonNull List<OptionGroup> parentList) {
            super(parentList);
        }

        @NonNull
        @Override
        public OptionGroupViewHolder onCreateParentViewHolder(@NonNull ViewGroup parentViewGroup, int viewType) {
            return new OptionGroupViewHolder(LayoutInflater.from(parentViewGroup.getContext())
                    .inflate(R.layout.dialog_code_generate_option_group, parentViewGroup, false));
        }

        @NonNull
        @Override
        public OptionViewHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
            return new OptionViewHolder(LayoutInflater.from(childViewGroup.getContext())
                    .inflate(R.layout.dialog_code_generate_option, childViewGroup, false));
        }

        @Override
        public void onBindParentViewHolder(@NonNull OptionGroupViewHolder viewHolder, int parentPosition, @NonNull OptionGroup optionGroup) {
            viewHolder.title.setText(optionGroup.titleRes);
            viewHolder.icon.setRotation(viewHolder.isExpanded() ? 0 : -90);
        }

        @Override
        public void onBindChildViewHolder(@NonNull OptionViewHolder viewHolder, int parentPosition, int childPosition, @NonNull Option option) {
            viewHolder.title.setText(option.titleRes);
            viewHolder.checkBox.setChecked(option.checked, false);
        }
    }

}
