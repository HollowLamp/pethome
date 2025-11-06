import "./App.css";
import { RouterProvider } from "react-router";
import router from "./router/index";
import { ConfigProvider, App as AntdApp } from "antd";

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#FF8A34",
          colorSuccess: "#4CAF50",
          colorError: "#F44336",
          colorWarning: "#FF9800",
          colorInfo: "#2196F3",

          fontSize: 14,
          borderRadius: 8,
          colorBgBase: "#FAFAFA",
          colorTextBase: "#333",
        },
      }}
    >
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  );
}

export default App;
