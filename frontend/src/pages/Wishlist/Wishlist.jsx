import React, { useEffect, useState } from "react";
import { Card, Empty, Skeleton, App as AntdApp, Typography } from "antd";
import { useNavigate } from "react-router";
import api from "../../api";
import PetCard from "../Pets/components/PetCard";

const { Title } = Typography;

export default function Wishlist() {
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [pets, setPets] = useState([]);

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.pets.getWishlistPets();
      if (res?.code === 200) {
        setPets(res.data || []);
      }
    } catch (e) {
      message.error(e?.message || "获取愿望单失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Card style={{ marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          我的愿望单
        </Title>
      </Card>
      {loading ? (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
            gap: 24,
          }}
        >
          {Array.from({ length: 8 }).map((_, i) => (
            <Card key={i}>
              <Skeleton active paragraph={{ rows: 2 }} />
            </Card>
          ))}
        </div>
      ) : pets.length > 0 ? (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
            gap: 24,
          }}
        >
          {pets.map((pet) => (
            <PetCard
              key={pet.id}
              pet={pet}
              onClick={() => navigate(`/pets/${pet.id}`)}
            />
          ))}
        </div>
      ) : (
        <Empty description="愿望单为空，去发现心动的伙伴吧~" />
      )}
    </div>
  );
}
