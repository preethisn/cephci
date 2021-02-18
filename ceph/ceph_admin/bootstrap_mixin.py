"""
Cephadm Bootstrap the ceph cluster
"""

import logging

from utility.utils import get_cephci_config

logger = logging.getLogger(__name__)



class BootstrapMixin:

    def get_method(self, key):
        verify_map = {
            "fsid": self.validate_fsid,
        }
        return verify_map.get(key)

    def bootstrap(self):
        """
        Bootstrap the ceph cluster with supported options

        Bootstrap involves,
          - Creates /etc/ceph directory with permissions
          - CLI creation with bootstrap options with custom/default image
          - Execution of bootstrap command
        """
        # copy ssh keys to other hosts
        self.cluster.setup_ssh_keys()

        # set tool download repository
        self.set_tool_repo()

        # install/download cephadm package on installer
        self.install_cephadm()

        # Create and set permission to ceph directory
        self.installer.exec_command(sudo=True, cmd="mkdir -p /etc/ceph")
        self.installer.exec_command(sudo=True, cmd="chmod 777 /etc/ceph")

        # Execute bootstrap with MON ip-address
        # Construct bootstrap command
        # 1) Skip default mon, mgr & crash specs
        # 2) Skip automatic dashboard provisioning
        cdn_cred = get_cephci_config().get("cdn_credentials")

        cmd = "cephadm -v "
        if not self.config.get("registry") and self.config.get("container_image"):
            cmd += "--image {image} ".format(image=self.config.get("container_image"))

        cmd += (
            "bootstrap "
            "--registry-url registry.redhat.io "
            "--registry-username {user} "
            "--registry-password {password} "
            "--orphan-initial-daemons "
            "--skip-monitoring-stack "
            "--mon-ip {mon_ip}"
        )



        cmd = cmd.format(
            user=cdn_cred.get("username"),
            password=cdn_cred.get("password"),
            mon_ip=self.installer.node.ip_address,
        )
        if self.config.get("args",{}).get("fsid"):
            cmd += " --fsid " + str(self.config.get("args",{}).get("fsid"))

        out, err = self.installer.exec_command(
            sudo=True,
            cmd=cmd,
            timeout=1800,
            check_ec=True,
        )

        logger.info("Bootstrap output : %s", out.read().decode())
        logger.error("Bootstrap error: %s", err.read().decode())


#        if self.config.get("fsid"):
#           self.validate_fsid()

        self.distribute_cephadm_gen_pub_key()


    # Verification of arguments

        args = self.config.get("args", {})
        logger.info(args)
        for key, value in args.items():
            func = self.get_method(key)
            if not func:
                continue
            if not func(value):
                raise AssertionError

    def validate_fsid(self, fsid) -> bool:
        out, err = self.shell(remote=self.installer, args = ['ceph', 'fsid'])
        logger.info("Installed with custom fsid %s", fsid)
        return out == fsid
