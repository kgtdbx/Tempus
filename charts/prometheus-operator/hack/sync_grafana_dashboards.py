#!/usr/bin/env python3
#
# Copyright © 2016-2018 The Thingsboard Authors
# Modifications © 2017-2018 Hashmap, Inc
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Fetch dashboards from provided urls into this chart."""
import json
import textwrap
from os import makedirs, path

import requests
import yaml
from yaml.representer import SafeRepresenter


# https://stackoverflow.com/a/20863889/961092
class LiteralStr(str):
    pass


def change_style(style, representer):
    def new_representer(dumper, data):
        scalar = representer(dumper, data)
        scalar.style = style
        return scalar

    return new_representer


# Source files list
charts = [
    {
        'source': 'https://raw.githubusercontent.com/coreos/prometheus-operator/master/contrib/kube-prometheus/manifests/grafana-dashboardDefinitions.yaml',
        'destination': '../templates/grafana/dashboards',
        'type': 'yaml',
    },
    {
        'source': 'https://raw.githubusercontent.com/etcd-io/etcd/master/Documentation/op-guide/grafana.json',
        'destination': '../templates/grafana/dashboards',
        'type': 'json',
    },
]

# Additional conditions map
condition_map = {
    'grafana-coredns-k8s': ' .Values.coreDns.enabled',
    'etcd': ' .Values.kubeEtcd.enabled',
}

# standard header
header = '''# Generated from '%(name)s' from %(url)s
{{- if and .Values.grafana.enabled .Values.grafana.defaultDashboardsEnabled%(condition)s }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ printf "%%s-%%s" (include "prometheus-operator.fullname" $) "%(name)s" | trunc 63 | trimSuffix "-" }}
  labels:
    {{- if $.Values.grafana.sidecar.dashboards.label }}
    {{ $.Values.grafana.sidecar.dashboards.label }}: "1"
    {{- end }}
    app: {{ template "prometheus-operator.name" $ }}-grafana
{{ include "prometheus-operator.labels" $ | indent 4 }}
data:
'''


def init_yaml_styles():
    represent_literal_str = change_style('|', SafeRepresenter.represent_str)
    yaml.add_representer(LiteralStr, represent_literal_str)


def escape(s):
    return s.replace("{{", "{{`{{").replace("}}", "}}`}}")


def yaml_str_repr(struct, indent=2):
    """represent yaml as a string"""
    text = yaml.dump(
        struct,
        width=1000,  # to disable line wrapping
        default_flow_style=False  # to disable multiple items on single line
    )
    text = escape(text)  # escape {{ and }} for helm
    text = textwrap.indent(text, ' ' * indent)
    return text


def write_group_to_file(resource_name, content, url, destination):
    # initialize header
    lines = header % {
        'name': resource_name,
        'url': url,
        'condition': condition_map.get(resource_name, ''),
    }

    filename_struct = {resource_name + '.json': (LiteralStr(content))}
    # rules themselves
    lines += yaml_str_repr(filename_struct)

    # footer
    lines += '{{- end }}'

    filename = resource_name + '.yaml'
    new_filename = "%s/%s" % (destination, filename)

    # make sure directories to store the file exist
    makedirs(destination, exist_ok=True)

    # recreate the file
    with open(new_filename, 'w') as f:
        f.write(lines)

    print("Generated %s" % new_filename)


def main():
    init_yaml_styles()
    # read the rules, create a new template file per group
    for chart in charts:
        print("Generating rules from %s" % chart['source'])
        response = requests.get(chart['source'])
        if response.status_code != 200:
            print('Skipping the file, response code %s not equals 200' % response.status_code)
            continue
        raw_text = response.text
        if chart['type'] == 'yaml':
            yaml_text = yaml.load(raw_text)
            groups = yaml_text['items']
            for group in groups:
                for resource, content in group['data'].items():
                    write_group_to_file(resource.replace('.json', ''), content, chart['source'], chart['destination'])
        elif chart['type'] == 'json':
            json_text = json.loads(raw_text)
            # is it already a dashboard structure or is it nested (etcd case)?
            flat_structure = bool(json_text.get('annotations'))
            if flat_structure:
                resource = path.basename(chart['source']).replace('.json', '')
                write_group_to_file(resource, json.dumps(json_text, indent=4), chart['source'], chart['destination'])
            else:
                for resource, content in json_text.items():
                    write_group_to_file(resource.replace('.json', ''), json.dumps(content, indent=4), chart['source'], chart['destination'])
    print("Finished")


if __name__ == '__main__':
    main()
