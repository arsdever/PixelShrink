from conan import ConanFile
from conan.tools.cmake import CMakeDeps, CMakeToolchain, cmake_layout


class PixelShrinkConan(ConanFile):
    settings = "os", "compiler", "build_type", "arch"
    options = {"with_tests": [True, False]}
    default_options = {"with_tests": False}
    requires = "libjpeg-turbo/3.2.0"
    generators = "CMakeDeps", "CMakeToolchain"

    def build_requirements(self):
        if self.options.with_tests:
            self.test_requires("gtest/1.18.0")

    def layout(self):
        cmake_layout(self)
