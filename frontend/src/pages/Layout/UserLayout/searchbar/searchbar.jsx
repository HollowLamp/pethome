import React from "react";
import { Input } from "antd";
import { SearchOutlined } from "@ant-design/icons";

const suffix = <SearchOutlined />;

export default function Searchbar() {
  return (
    <div>
      <Input suffix={suffix} style={{ width: 220 }} />
    </div>
  );
}
