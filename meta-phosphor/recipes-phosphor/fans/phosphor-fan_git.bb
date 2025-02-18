SUMMARY = "Phosphor Fan"
DESCRIPTION = "Phosphor fan provides a set of fan monitoring and \
control applications."
PR = "r1"
PV = "1.0+git${SRCPV}"

require ${BPN}.inc

inherit autotools pkgconfig python3native
inherit obmc-phosphor-systemd
inherit phosphor-fan

S = "${WORKDIR}/git"

# Common build dependencies
DEPENDS += "autoconf-archive-native"
DEPENDS += "${PYTHON_PN}-pyyaml-native"
DEPENDS += "${PYTHON_PN}-mako-native"
DEPENDS += "sdbusplus"
DEPENDS += "${PYTHON_PN}-sdbus++-native"
DEPENDS += "sdeventplus"
DEPENDS += "gpioplus"
DEPENDS += "phosphor-logging"
DEPENDS += "libevdev"
DEPENDS += "nlohmann-json"
DEPENDS += "cli11"

# Package configuration
FAN_PACKAGES = " \
        ${PN}-presence-tach \
        ${PN}-control \
        ${PN}-monitor \
        ${PN}-sensor-monitor \
"

ALLOW_EMPTY:${PN} = "1"
PACKAGE_BEFORE_PN += "${FAN_PACKAGES}"
PACKAGECONFIG ?= "presence control monitor sensor-monitor"
SYSTEMD_PACKAGES = "${FAN_PACKAGES}"
PKG_DEFAULT_MACHINE ??= "${MACHINE}"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# The control, monitor, and presence apps can either be JSON or YAML driven.
PACKAGECONFIG[json] = "--enable-json, --disable-json"

# --------------------------------------
# ${PN}-presence-tach specific configuration
PACKAGECONFIG[presence] = "--enable-presence \
    MACHINE=${PKG_DEFAULT_MACHINE} \
    PRESENCE_CONFIG=${STAGING_DIR_HOST}${presence_datadir}/config.yaml, \
    --disable-presence, \
    virtual/phosphor-fan-presence-config \
    , \
"

MULTI_USR_TGT = "multi-user.target"
TMPL_TACH = "phosphor-fan-presence-tach@.service"
INSTFMT_TACH = "phosphor-fan-presence-tach@{0}.service"
POWERON_TGT = "obmc-chassis-poweron@{0}.target"
FMT_TACH = "../${TMPL_TACH}:${POWERON_TGT}.wants/${INSTFMT_TACH}"
FMT_TACH_MUSR = "../${TMPL_TACH}:${MULTI_USR_TGT}.wants/${INSTFMT_TACH}"

FILES:${PN}-presence-tach = "${bindir}/phosphor-fan-presence-tach"
SYSTEMD_SERVICE:${PN}-presence-tach += "${TMPL_TACH}"
SYSTEMD_LINK:${PN}-presence-tach += "${@compose_list(d, 'FMT_TACH', 'OBMC_CHASSIS_INSTANCES')}"

# JSON mode also gets linked into multi-user
SYSTEMD_LINK:${PN}-presence-tach += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
        compose_list(d, 'FMT_TACH_MUSR', 'OBMC_CHASSIS_INSTANCES'), '', d)}"

# Package the JSON config files installed from the repo
FILES:${PN}-presence-tach += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
    '${datadir}/phosphor-fan-presence/presence/*', '', d)}"

# --------------------------------------
# ${PN}-control specific configuration
PACKAGECONFIG[control] = "--enable-control \
    MACHINE=${PKG_DEFAULT_MACHINE} \
    FAN_DEF_YAML_FILE=${STAGING_DIR_HOST}${control_datadir}/fans.yaml \
    FAN_ZONE_YAML_FILE=${STAGING_DIR_HOST}${control_datadir}/zones.yaml \
    ZONE_EVENTS_YAML_FILE=${STAGING_DIR_HOST}${control_datadir}/events.yaml \
    ZONE_CONDITIONS_YAML_FILE=${STAGING_DIR_HOST}${control_datadir}/zone_conditions.yaml, \
    --disable-control, \
    virtual/phosphor-fan-control-fan-config \
    phosphor-fan-control-zone-config \
    phosphor-fan-control-events-config \
    phosphor-fan-control-zone-conditions-config \
    , \
"

FAN_CONTROL_TGT = "obmc-fan-control-ready@{0}.target"

TMPL_CONTROL = "phosphor-fan-control@.service"
INSTFMT_CONTROL = "phosphor-fan-control@{0}.service"
FMT_CONTROL = "../${TMPL_CONTROL}:${FAN_CONTROL_TGT}.requires/${INSTFMT_CONTROL}"
FMT_CONTROL_MUSR = "../${TMPL_CONTROL}:${MULTI_USR_TGT}.wants/${INSTFMT_CONTROL}"
FMT_CONTROL_PWRON = "../${TMPL_CONTROL}:${POWERON_TGT}.wants/${INSTFMT_CONTROL}"

TMPL_CONTROL_INIT = "phosphor-fan-control-init@.service"
INSTFMT_CONTROL_INIT = "phosphor-fan-control-init@{0}.service"
FMT_CONTROL_INIT = "../${TMPL_CONTROL_INIT}:${POWERON_TGT}.wants/${INSTFMT_CONTROL_INIT}"

FILES:${PN}-control = "${bindir}/phosphor-fan-control"
FILES:${PN}-control += "${bindir}/fanctl"
SYSTEMD_SERVICE:${PN}-control += "${TMPL_CONTROL}"
SYSTEMD_SERVICE:${PN}-control += "${@bb.utils.contains('PACKAGECONFIG', 'json', '', '${TMPL_CONTROL_INIT}', d)}"

# JSON: Linked to multi-user and poweron
# YAML: Linked to fans-ready and fan control-init poweron
SYSTEMD_LINK:${PN}-control += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
        compose_list(d, 'FMT_CONTROL_MUSR', 'OBMC_CHASSIS_INSTANCES'), \
        compose_list(d, 'FMT_CONTROL', 'OBMC_CHASSIS_INSTANCES'), d)}"
SYSTEMD_LINK:${PN}-control += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
        compose_list(d, 'FMT_CONTROL_PWRON', 'OBMC_CHASSIS_INSTANCES'), \
        compose_list(d, 'FMT_CONTROL_INIT', 'OBMC_CHASSIS_INSTANCES'), d)}"

# Package the JSON config files installed from the repo
FILES:${PN}-control += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
    '${datadir}/phosphor-fan-presence/control/*', '', d)}"

# --------------------------------------
# ${PN}-monitor specific configuration
PACKAGECONFIG[monitor] = "--enable-monitor \
    MACHINE=${PKG_DEFAULT_MACHINE} \
    FAN_MONITOR_YAML_FILE=${STAGING_DIR_HOST}${monitor_datadir}/monitor.yaml, \
    --disable-monitor, \
    phosphor-fan-monitor-config \
    , \
"

TMPL_MONITOR = "phosphor-fan-monitor@.service"
INSTFMT_MONITOR = "phosphor-fan-monitor@{0}.service"
FMT_MONITOR_FANSREADY = "../${TMPL_MONITOR}:${FAN_CONTROL_TGT}.requires/${INSTFMT_MONITOR}"
FMT_MONITOR_PWRON = "../${TMPL_MONITOR}:${POWERON_TGT}.wants/${INSTFMT_MONITOR}"
FMT_MONITOR_MUSR = "../${TMPL_MONITOR}:${MULTI_USR_TGT}.wants/${INSTFMT_MONITOR}"

TMPL_MONITOR_INIT = "phosphor-fan-monitor-init@.service"
INSTFMT_MONITOR_INIT = "phosphor-fan-monitor-init@{0}.service"
FMT_MONITOR_INIT = "../${TMPL_MONITOR_INIT}:${POWERON_TGT}.wants/${INSTFMT_MONITOR_INIT}"

FILES:${PN}-monitor = "${bindir}/phosphor-fan-monitor"
SYSTEMD_SERVICE:${PN}-monitor += "${TMPL_MONITOR}"
SYSTEMD_SERVICE:${PN}-monitor += "${@bb.utils.contains('PACKAGECONFIG', 'json', '', '${TMPL_MONITOR_INIT}', d)}"

# JSON: power on and multi-user links.  YAML: fans-ready and fan monitor init links
SYSTEMD_LINK:${PN}-monitor += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
                                compose_list(d, 'FMT_MONITOR_PWRON', 'OBMC_CHASSIS_INSTANCES'), \
                                compose_list(d, 'FMT_MONITOR_FANSREADY', 'OBMC_CHASSIS_INSTANCES'), d)}"

SYSTEMD_LINK:${PN}-monitor += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
                                compose_list(d, 'FMT_MONITOR_MUSR', 'OBMC_CHASSIS_INSTANCES'), \
                                compose_list(d, 'FMT_MONITOR_INIT', 'OBMC_CHASSIS_INSTANCES'), d)}"

# Package the JSON config files installed from the repo
FILES:${PN}-monitor += "${@bb.utils.contains('PACKAGECONFIG', 'json', \
    '${datadir}/phosphor-fan-presence/monitor/*', '', d)}"

# --------------------------------------
# phosphor-cooling-type specific configuration
PACKAGECONFIG[cooling-type] = "--enable-cooling-type,--disable-cooling-type,,"

# --------------------------------------
# ${PN}-sensor-monitor specific configuration
PACKAGECONFIG[sensor-monitor] = "--enable-sensor-monitor, --disable-sensor-monitor"

FAN_PACKAGES:append = "${@bb.utils.contains('PACKAGECONFIG', 'sensor-monitor', ' sensor-monitor', '', d)}"

FILES:sensor-monitor += " ${bindir}/sensor-monitor"
SYSTEMD_SERVICE:sensor-monitor += "sensor-monitor.service"
SYSTEMD_LINK:sensor-monitor += "../sensor-monitor.service:${MULTI_USR_TGT}.wants/sensor-monitor.service"
