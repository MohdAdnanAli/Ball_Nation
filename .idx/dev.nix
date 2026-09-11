{ pkgs, ... }: {
  # packages is the list of packages that will be available in the environment
  packages = [
    pkgs.sudo
    pkgs.jdk8
  ];
}
