/**
 * The modules of the app a plugin may import, by their path below projectforge-next (`components/shared/page-shell`,
 * written as `@/components/shared/page-shell` in the plugin) or by their package name. A plugin build maps these imports to
 * the instances below (see packages/plugin-vite), so it renders with the very components, hooks and clients of the app,
 * and the context they share (query client, texts, unsaved-changes dialog, ...).
 *
 * **This list is the contract:** a module is on it because a plugin needs it, and what a plugin imports from it may not
 * be renamed or removed without raising PLUGIN_SDK_VERSION (lib/plugin-runtime/manifest.ts); a test pins the list. Nothing
 * else of the app is reachable for a plugin.
 */
import * as dataTable from "@/components/data-table";
import * as dataTableFilterPillShell from "@/components/data-table/filter-pill-shell";
import * as dataTableUseKeyboardNav from "@/components/data-table/use-keyboard-nav";
import * as featuresChatChatPanel from "@/components/features/chat/chat-panel";
import * as sharedAttachmentsAttachmentDropArea from "@/components/shared/attachments/attachment-drop-area";
import * as sharedChartChartValueTooltip from "@/components/shared/chart/chart-value-tooltip";
import * as sharedConfirmDialog from "@/components/shared/confirm-dialog";
import * as sharedCopyButton from "@/components/shared/copy-button";
import * as sharedCopyableCollapsibleTrigger from "@/components/shared/copyable-collapsible-trigger";
import * as sharedCopyableSelect from "@/components/shared/copyable-select";
import * as sharedDownloadButton from "@/components/shared/download-button";
import * as sharedEditPageTabs from "@/components/shared/edit-page-tabs";
import * as sharedEditEntityEditPage from "@/components/shared/edit/entity-edit-page";
import * as sharedEntityAutocomplete from "@/components/shared/entity-autocomplete";
import * as sharedEntityMultiAutocomplete from "@/components/shared/entity-multi-autocomplete";
import * as sharedFormAlert from "@/components/shared/form-alert";
import * as sharedFormFieldHint from "@/components/shared/form/field-hint";
import * as sharedFormFormContext from "@/components/shared/form/form-context";
import * as sharedFormTextAreaField from "@/components/shared/form/text-area-field";
import * as sharedGuardedDialog from "@/components/shared/guarded-dialog";
import * as sharedHintTooltip from "@/components/shared/hint-tooltip";
import * as sharedJobsJobProgress from "@/components/shared/jobs/job-progress";
import * as sharedKpiTile from "@/components/shared/kpi-tile";
import * as sharedListEntityListPage from "@/components/shared/list/entity-list-page";
import * as sharedListSearchInput from "@/components/shared/list/search-input";
import * as sharedPageShell from "@/components/shared/page-shell";
import * as sharedPageTitleRow from "@/components/shared/page-title-row";
import * as sharedRichText from "@/components/shared/rich-text";
import * as sharedRichTextEditor from "@/components/shared/rich-text-editor";
import * as sharedRightsHint from "@/components/shared/rights-hint";
import * as sharedSectionCard from "@/components/shared/section-card";
import * as sharedSectionNav from "@/components/shared/section-nav";
import * as sharedSpinner from "@/components/shared/spinner";
import * as sharedStatisticsLineClass from "@/components/shared/statistics-line-class";
import * as sharedStatusPill from "@/components/shared/status-pill";
import * as uiBadge from "@/components/ui/badge";
import * as uiButton from "@/components/ui/button";
import * as uiChart from "@/components/ui/chart";
import * as uiCheckbox from "@/components/ui/checkbox";
import * as uiCollapsible from "@/components/ui/collapsible";
import * as uiDialog from "@/components/ui/dialog";
import * as uiDropdownMenu from "@/components/ui/dropdown-menu";
import * as uiInput from "@/components/ui/input";
import * as uiLabel from "@/components/ui/label";
import * as uiProgress from "@/components/ui/progress";
import * as uiSelect from "@/components/ui/select";
import * as uiTabs from "@/components/ui/tabs";
import * as uiTextarea from "@/components/ui/textarea";
import * as uiToggleGroup from "@/components/ui/toggle-group";
import * as hookUseCoarsePointer from "@/hooks/use-coarse-pointer";
import * as hookUseCurrentUserRef from "@/hooks/use-current-user-ref";
import * as hookUseFormat from "@/hooks/use-format";
import * as hookUseRouteParams from "@/hooks/use-route-params";
import * as hookUseTabParam from "@/hooks/use-tab-param";
import * as i18nConfig from "@/i18n/config";
import * as libBase64url from "@/lib/base64url";
import * as libChartsRoles from "@/lib/charts/roles";
import * as libDocsLinks from "@/lib/docs-links";
import * as libDynamicGridRowClass from "@/lib/dynamic/grid/row-class";
import * as libFormat from "@/lib/format";
import * as libLeafKey from "@/lib/leaf-key";
import * as libMarkdownToHtml from "@/lib/markdown-to-html";
import * as libPageDefDefinePage from "@/lib/page-def/define-page";
import * as libRsChat from "@/lib/rs/chat";
import * as libRsClient from "@/lib/rs/client";
import * as libRsDownload from "@/lib/rs/download";
import * as libRsJobs from "@/lib/rs/jobs";
import * as libRsUpload from "@/lib/rs/upload";
import * as libToast from "@/lib/toast";
import * as libUserZone from "@/lib/user-zone";
import * as libUtils from "@/lib/utils";
import * as libValidationFromMetadata from "@/lib/validation/from-metadata";
import * as storeJobStore from "@/store/job-store";
import * as nextNavigation from "next/navigation";
import * as nextLink from "next/link";
import * as zod from "zod";
import * as reactTable from "@tanstack/react-table";
import * as recharts from "recharts";

export const PLUGIN_MODULES: Record<string, Record<string, unknown>> = {
  "components/data-table": dataTable,
  "components/data-table/filter-pill-shell": dataTableFilterPillShell,
  "components/data-table/use-keyboard-nav": dataTableUseKeyboardNav,
  "components/features/chat/chat-panel": featuresChatChatPanel,
  "components/shared/attachments/attachment-drop-area":
    sharedAttachmentsAttachmentDropArea,
  "components/shared/chart/chart-value-tooltip": sharedChartChartValueTooltip,
  "components/shared/confirm-dialog": sharedConfirmDialog,
  "components/shared/copy-button": sharedCopyButton,
  "components/shared/copyable-collapsible-trigger":
    sharedCopyableCollapsibleTrigger,
  "components/shared/copyable-select": sharedCopyableSelect,
  "components/shared/download-button": sharedDownloadButton,
  "components/shared/edit-page-tabs": sharedEditPageTabs,
  "components/shared/edit/entity-edit-page": sharedEditEntityEditPage,
  "components/shared/entity-autocomplete": sharedEntityAutocomplete,
  "components/shared/entity-multi-autocomplete": sharedEntityMultiAutocomplete,
  "components/shared/form-alert": sharedFormAlert,
  "components/shared/form/field-hint": sharedFormFieldHint,
  "components/shared/form/form-context": sharedFormFormContext,
  "components/shared/form/text-area-field": sharedFormTextAreaField,
  "components/shared/guarded-dialog": sharedGuardedDialog,
  "components/shared/hint-tooltip": sharedHintTooltip,
  "components/shared/jobs/job-progress": sharedJobsJobProgress,
  "components/shared/kpi-tile": sharedKpiTile,
  "components/shared/list/entity-list-page": sharedListEntityListPage,
  "components/shared/list/search-input": sharedListSearchInput,
  "components/shared/page-shell": sharedPageShell,
  "components/shared/page-title-row": sharedPageTitleRow,
  "components/shared/rich-text": sharedRichText,
  "components/shared/rich-text-editor": sharedRichTextEditor,
  "components/shared/rights-hint": sharedRightsHint,
  "components/shared/section-card": sharedSectionCard,
  "components/shared/section-nav": sharedSectionNav,
  "components/shared/spinner": sharedSpinner,
  "components/shared/statistics-line-class": sharedStatisticsLineClass,
  "components/shared/status-pill": sharedStatusPill,
  "components/ui/badge": uiBadge,
  "components/ui/button": uiButton,
  "components/ui/chart": uiChart,
  "components/ui/checkbox": uiCheckbox,
  "components/ui/collapsible": uiCollapsible,
  "components/ui/dialog": uiDialog,
  "components/ui/dropdown-menu": uiDropdownMenu,
  "components/ui/input": uiInput,
  "components/ui/label": uiLabel,
  "components/ui/progress": uiProgress,
  "components/ui/select": uiSelect,
  "components/ui/tabs": uiTabs,
  "components/ui/textarea": uiTextarea,
  "components/ui/toggle-group": uiToggleGroup,
  "hooks/use-coarse-pointer": hookUseCoarsePointer,
  "hooks/use-current-user-ref": hookUseCurrentUserRef,
  "hooks/use-format": hookUseFormat,
  "hooks/use-route-params": hookUseRouteParams,
  "hooks/use-tab-param": hookUseTabParam,
  "i18n/config": i18nConfig,
  "lib/base64url": libBase64url,
  "lib/charts/roles": libChartsRoles,
  "lib/docs-links": libDocsLinks,
  "lib/dynamic/grid/row-class": libDynamicGridRowClass,
  "lib/format": libFormat,
  "lib/leaf-key": libLeafKey,
  "lib/markdown-to-html": libMarkdownToHtml,
  "lib/page-def/define-page": libPageDefDefinePage,
  "lib/rs/chat": libRsChat,
  "lib/rs/client": libRsClient,
  "lib/rs/download": libRsDownload,
  "lib/rs/jobs": libRsJobs,
  "lib/rs/upload": libRsUpload,
  "lib/toast": libToast,
  "lib/user-zone": libUserZone,
  "lib/utils": libUtils,
  "lib/validation/from-metadata": libValidationFromMetadata,
  "store/job-store": storeJobStore,
  "next/navigation": nextNavigation,
  "next/link": nextLink,
  zod: zod,
  "@tanstack/react-table": reactTable,
  // The charts of a plugin draw with the very recharts of components/ui/chart: a second copy has no context of the first.
  recharts: recharts,
};
