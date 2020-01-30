#!/usr/bin/env python3

# Wrapper to transform staging files for cBioPortal
# Author: Oleguer Plantalech, Sander Tan, Dionne Zaal, Sander Rodenburg, The Hyve

import os
import argparse
import sys

if __name__ == '__main__':
    parser = argparse.ArgumentParser(usage="-i <dir_for_input_files> -o <dir_for_output_files>",
                                     description="Transforms all files for all studies in input folder to cBioPortal "
                                                 "staging files")

    arguments = parser.add_argument_group('Named arguments')

    arguments.add_argument("-i", "--input_dir",
                           required=True,
                           help="Directory containing input files.")

    arguments.add_argument("-o", "--output_dir",
                           required=True,
                           help="Directory for output files.")

    args = parser.parse_args()
    os.system("tar -C " + args.output_dir + " -xzvf " + args.input_dir + "/test_study_es_0.tar.gz")
    sys.exit(0)
