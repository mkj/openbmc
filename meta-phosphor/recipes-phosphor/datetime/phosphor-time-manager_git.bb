SUMMARY = "Phosphor Time Manager daemon"
DESCRIPTION = "Daemon to cater to BMC and HOST time management"
HOMEPAGE = "http://github.com/openbmc/phosphor-time-manager"
PR = "r1"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e3fc50a88d0a364313df4b21ef20c29e"
inherit meson pkgconfig python3native
inherit obmc-phosphor-dbus-service

DEPENDS += "autoconf-archive-native"
DEPENDS += "phosphor-mapper"
DEPENDS += "systemd"
DEPENDS += "sdbusplus"
DEPENDS += "${PYTHON_PN}-sdbus++-native"
DEPENDS += "phosphor-logging"
DEPENDS += "phosphor-dbus-interfaces"
RDEPENDS:${PN} += "phosphor-settings-manager"
RDEPENDS:${PN} += "phosphor-network"
RDEPENDS:${PN} += "phosphor-mapper"

SRC_URI += "git://github.com/openbmc/phosphor-time-manager;branch=master;protocol=https"
SRCREV = "3867926613f478afe5fa7e700ebe0cb293e88efd"
PV = "1.0+git${SRCPV}"
S = "${WORKDIR}/git"

DBUS_SERVICE:${PN} += "xyz.openbmc_project.Time.Manager.service"
